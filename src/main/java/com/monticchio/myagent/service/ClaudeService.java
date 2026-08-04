package com.monticchio.myagent.service;

import com.monticchio.myagent.dto.AnthropicDtos.*;
import com.monticchio.myagent.entity.Conversation;
import com.monticchio.myagent.entity.Message;
import com.monticchio.myagent.entity.User;
import com.monticchio.myagent.exception.LlmException;
import com.monticchio.myagent.repository.MessageRepository;
import com.monticchio.myagent.tool.ToolRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ClaudeService {

    private static final int MAX_TOOL_ITERATIONS = 5;

    private final RestClient restClient;
    private final String model;
    private final String systemPrompt;
    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final QuotaService quotaService;
    private final ToolRegistry toolRegistry;

    public ClaudeService(
            @Value("${anthropic.api.key}") String apiKey,
            @Value("${anthropic.api.url}") String apiUrl,
            @Value("${anthropic.model}") String model,
            MessageRepository messageRepository,
            ConversationService conversationService,
            QuotaService quotaService,
            ToolRegistry toolRegistry) {

        this.model = model;
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.quotaService = quotaService;
        this.toolRegistry = toolRegistry;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();

        try (InputStream in = getClass().getResourceAsStream("/system-prompt.txt")) {
            this.systemPrompt = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load system-prompt.txt", e);
        }
    }

    public record ChatResult(Long conversationId, String reply) {}

    public record ImageAttachment(byte[] data, String mediaType, String filename) {}

    public ChatResult chat(String username, Long conversationId, String userMessage) {
        return chat(username, conversationId, userMessage, List.of());
    }

    public ChatResult chat(String username, Long conversationId, String userMessage, List<ImageAttachment> images) {
        User user = conversationService.getUser(username);
        quotaService.checkAndConsume(user);

        Conversation conversation = conversationService.resolveOwnedConversation(username, conversationId);

        // Images are never persisted to the DB (Message.content is a plain String column, and
        // there's no need to "re-see" a past photo in later turns): only a text placeholder is
        // saved, while the raw bytes are used for this request's Anthropic call below and then
        // discarded. The model's own full-text diagnosis reply IS persisted as usual, so later
        // turns (e.g. "how's the treatment going?") still have everything needed to connect back
        // to what was diagnosed from the photo, purely through normal conversation memory.
        String persistedContent = images.isEmpty()
                ? userMessage
                : "[Images attached: " + String.join(", ", images.stream().map(ImageAttachment::filename).toList())
                        + "] " + (userMessage == null ? "" : userMessage);

        Message userMsg = new Message();
        userMsg.setConversation(conversation);
        userMsg.setRole("user");
        userMsg.setContent(persistedContent);
        messageRepository.save(userMsg);

        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // In-memory working list only: intermediate tool_use/tool_result turns
        // are never persisted — only the user message (already saved above)
        // and the final text reply (saved at the end of this method).
        List<ChatMessage> messages = new ArrayList<>(history.stream()
                .map(m -> new ChatMessage(m.getRole(), m.getContent()))
                .toList());

        if (!images.isEmpty()) {
            List<ContentBlock> currentTurnContent = new ArrayList<>();
            for (ImageAttachment image : images) {
                String base64Data = Base64.getEncoder().encodeToString(image.data());
                currentTurnContent.add(new ContentBlock("image", null, null, null, null, null, null,
                        new ImageSource("base64", image.mediaType(), base64Data)));
            }
            if (userMessage != null && !userMessage.isBlank()) {
                currentTurnContent.add(new ContentBlock("text", userMessage, null, null, null, null, null, null));
            }
            messages.set(messages.size() - 1, new ChatMessage("user", currentTurnContent));
        }

        List<ToolDefinition> tools = toolRegistry.toolDefinitions();
        String effectiveSystemPrompt = buildSystemPrompt(conversation);

        String finalText = null;

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            AnthropicResponse response = callAnthropic(new AnthropicRequest(model, 1024, messages, tools, effectiveSystemPrompt));

            if (!"tool_use".equals(response.stop_reason())) {
                finalText = response.content().stream()
                        .filter(block -> "text".equals(block.type()))
                        .map(ContentBlock::text)
                        .findFirst()
                        .orElse("(no response)");
                break;
            }

            List<ContentBlock> toolUses = response.content().stream()
                    .filter(block -> "tool_use".equals(block.type()))
                    .toList();

            if (toolUses.isEmpty()) {
                throw new LlmException("Model reported tool_use but did not provide any tool_use block");
            }

            // Executed one at a time, in the order Claude requested them: our tools are all
            // fast (local lookups or a single HTTP call), so real concurrency isn't worth the
            // added complexity of multi-threaded exception handling and service thread-safety.
            List<ContentBlock> toolResults = toolUses.stream()
                    .map(toolUse -> {
                        String result = toolRegistry.execute(toolUse.name(), toolUse.input());
                        return new ContentBlock("tool_result", null, null, null, null, toolUse.id(), result, null);
                    })
                    .toList();

            messages.add(new ChatMessage("assistant", response.content()));
            messages.add(new ChatMessage("user", toolResults));
        }

        if (finalText == null) {
            throw new LlmException("Maximum tool iteration count exceeded (" + MAX_TOOL_ITERATIONS + ")");
        }

        Message assistantMsg = new Message();
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(finalText);
        messageRepository.save(assistantMsg);

        return new ChatResult(conversation.getId(), finalText);
    }

    private String buildSystemPrompt(Conversation conversation) {
        if (conversation.getTitle() == null) {
            return systemPrompt;
        }
        String plantationNote = conversation.getPlantationLabel() != null
                ? ", part of the \"" + conversation.getPlantationLabel() + "\" plantation/grove alongside other tracked plants"
                : ", grown as a standalone plant not part of any tracked plantation/grove";
        return systemPrompt + "\n\nThis conversation is the dedicated case file for a specific plant named \""
                + conversation.getTitle() + "\"" + plantationNote + ". Treat the conversation history above as "
                + "that plant's ongoing record and let it inform your diagnosis and treatment advice over time.";
    }

    private AnthropicResponse callAnthropic(AnthropicRequest request) {
        AnthropicResponse response;
        try {
            response = restClient.post()
                    .body(request)
                    .retrieve()
                    .body(AnthropicResponse.class);
        } catch (RestClientException e) {
            throw new LlmException("Error calling Anthropic API", e);
        }
        if (response == null || response.content() == null) {
            throw new LlmException("Empty response from Anthropic API");
        }
        return response;
    }
}
