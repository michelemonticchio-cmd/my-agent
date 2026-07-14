package com.monticchio.myagent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public class AnthropicDtos {

    public record AnthropicRequest(
            String model,
            int max_tokens,
            List<ChatMessage> messages,
            List<ToolDefinition> tools,
            String system
    ) {}

    public record ChatMessage(String role, Object content) {} // content: String or List<ContentBlock>

    public record ToolDefinition(
            String name,
            String description,
            Map<String, Object> input_schema
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(
            String type,
            String text,                          // for type="text"
            String id, String name, Object input,  // for type="tool_use"
            String tool_use_id, Object content      // for type="tool_result"
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnthropicResponse(List<ContentBlock> content, String stop_reason) {}
}
