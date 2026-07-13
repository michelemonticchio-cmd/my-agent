package com.monticchio.myagent.controller;

import com.monticchio.myagent.service.ClaudeService;
import com.monticchio.myagent.service.ClaudeService.ChatResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ClaudeService claudeService;

    public ChatController(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    public record ChatRequest(Long conversationId, String message) {}
    public record ChatResponse(Long conversationId, String reply) {}

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        ChatResult result = claudeService.chat(request.conversationId(), request.message());
        return new ChatResponse(result.conversationId(), result.reply());
    }
}