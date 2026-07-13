package com.monticchio.myagent.service;

import com.monticchio.myagent.dto.AnthropicDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class ClaudeService {

    private final RestClient restClient;
    private final String model;

    public ClaudeService(
            @Value("${anthropic.api.key}") String apiKey,
            @Value("${anthropic.api.url}") String apiUrl,
            @Value("${anthropic.model}") String model) {

        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    public String chat(String userMessage) {
        AnthropicRequest request = new AnthropicRequest(
                model,
                1024,
                List.of(new ChatMessage("user", userMessage))
        );

        AnthropicResponse response = restClient.post()
                .body(request)
                .retrieve()
                .body(AnthropicResponse.class);

        return response.content().stream()
                .filter(block -> "text".equals(block.type()))
                .map(ContentBlock::text)
                .findFirst()
                .orElse("(nessuna risposta)");
    }
}