package com.monticchio.myagent.service;

import com.monticchio.myagent.dto.AnthropicDtos.*;
import com.monticchio.myagent.entity.Conversation;
import com.monticchio.myagent.entity.Message;
import com.monticchio.myagent.exception.LlmException;
import com.monticchio.myagent.repository.ConversationRepository;
import com.monticchio.myagent.repository.MessageRepository;
import com.monticchio.myagent.tool.ToolRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClaudeService {

    private static final int MAX_TOOL_ITERATIONS = 5;

    private final RestClient restClient;
    private final String model;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ToolRegistry toolRegistry;

    public ClaudeService(
            @Value("${anthropic.api.key}") String apiKey,
            @Value("${anthropic.api.url}") String apiUrl,
            @Value("${anthropic.model}") String model,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ToolRegistry toolRegistry) {

        this.model = model;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.toolRegistry = toolRegistry;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    public record ChatResult(Long conversationId, String reply) {}

    public ChatResult chat(Long conversationId, String userMessage) {
        Conversation conversation = conversationId == null
                ? conversationRepository.save(new Conversation())
                : conversationRepository.findById(conversationId)
                        .orElseThrow(() -> new LlmException("Conversazione non trovata"));

        Message userMsg = new Message();
        userMsg.setConversation(conversation);
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        messageRepository.save(userMsg);

        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // Lista di lavoro solo in memoria: i turni intermedi tool_use/tool_result
        // non vengono mai persistiti — solo il messaggio utente (già salvato sopra)
        // e la risposta testuale finale (salvata a fine metodo).
        List<ChatMessage> messages = new ArrayList<>(history.stream()
                .map(m -> new ChatMessage(m.getRole(), m.getContent()))
                .toList());

        List<ToolDefinition> tools = toolRegistry.toolDefinitions();

        String finalText = null;

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            AnthropicResponse response = callAnthropic(new AnthropicRequest(model, 1024, messages, tools));

            if (!"tool_use".equals(response.stop_reason())) {
                finalText = response.content().stream()
                        .filter(block -> "text".equals(block.type()))
                        .map(ContentBlock::text)
                        .findFirst()
                        .orElse("(nessuna risposta)");
                break;
            }

            ContentBlock toolUse = response.content().stream()
                    .filter(block -> "tool_use".equals(block.type()))
                    .findFirst()
                    .orElseThrow(() -> new LlmException("Il modello ha segnalato tool_use senza fornire un blocco tool_use"));

            String result = toolRegistry.execute(toolUse.name(), toolUse.input());

            messages.add(new ChatMessage("assistant", response.content()));
            messages.add(new ChatMessage("user", List.of(
                    new ContentBlock("tool_result", null, null, null, null, toolUse.id(), result)
            )));
        }

        if (finalText == null) {
            throw new LlmException("Numero massimo di iterazioni tool superato (" + MAX_TOOL_ITERATIONS + ")");
        }

        Message assistantMsg = new Message();
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(finalText);
        messageRepository.save(assistantMsg);

        return new ChatResult(conversation.getId(), finalText);
    }

    private AnthropicResponse callAnthropic(AnthropicRequest request) {
        AnthropicResponse response;
        try {
            response = restClient.post()
                    .body(request)
                    .retrieve()
                    .body(AnthropicResponse.class);
        } catch (RestClientException e) {
            throw new LlmException("Errore nella chiamata all'API Anthropic", e);
        }
        if (response == null || response.content() == null) {
            throw new LlmException("Risposta vuota dall'API Anthropic");
        }
        return response;
    }
}
