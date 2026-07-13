package com.monticchio.myagent.controller;

import com.monticchio.myagent.service.ClaudeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ClaudeService claudeService;

    public ChatController(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    public record ChatRequest(String message) {}
    public record ChatResponse(String reply) {}

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = claudeService.chat(request.message());
        return new ChatResponse(reply);
    }
}