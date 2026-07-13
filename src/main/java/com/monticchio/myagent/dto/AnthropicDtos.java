package com.monticchio.myagent.dto;

import java.util.List;

public class AnthropicDtos {

    public record AnthropicRequest(
            String model,
            int max_tokens,
            List<ChatMessage> messages
    ) {}

    public record ChatMessage(String role, String content) {}

    public record AnthropicResponse(List<ContentBlock> content) {}

    public record ContentBlock(String type, String text) {}
}