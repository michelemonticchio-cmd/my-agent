package com.monticchio.myagent.service;

import com.monticchio.myagent.dto.AnthropicDtos.*;
import com.monticchio.myagent.entity.Conversation;
import com.monticchio.myagent.entity.Message;
import com.monticchio.myagent.exception.LlmException;
import com.monticchio.myagent.repository.ConversationRepository;
import com.monticchio.myagent.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class ClaudeService {

    private final RestClient restClient;
    private final String model;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ClaudeService(
            @Value("${anthropic.api.key}") String apiKey,
            @Value("${anthropic.api.url}") String apiUrl,
            @Value("${anthropic.model}") String model,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository) {

        this.model = model;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
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
        List<ChatMessage> chatMessages = history.stream()
                .map(m -> new ChatMessage(m.getRole(), m.getContent()))
                .toList();

        AnthropicRequest request = new AnthropicRequest(model, 1024, chatMessages);

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

        String reply = response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(ContentBlock::text)
                .findFirst()
                .orElse("(nessuna risposta)");

        Message assistantMsg = new Message();
        assistantMsg.setConversation(conversation);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(reply);
        messageRepository.save(assistantMsg);

        return new ChatResult(conversation.getId(), reply);
    }
}
