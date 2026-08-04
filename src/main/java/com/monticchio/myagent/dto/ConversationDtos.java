package com.monticchio.myagent.dto;

import java.time.Instant;

public class ConversationDtos {

    public record ConversationSummary(Long id, String title, String plantationLabel, Instant createdAt) {}

    public record CreateConversationRequest(String title, String plantationLabel) {}

    public record MessageDto(String role, String content, Instant createdAt) {}
}
