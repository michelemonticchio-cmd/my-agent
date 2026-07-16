package com.monticchio.myagent.controller;

import com.monticchio.myagent.exception.LlmException;
import com.monticchio.myagent.service.ClaudeService;
import com.monticchio.myagent.service.ClaudeService.ChatResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    @PostMapping(value = "/chat/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse chatWithImage(
            @RequestParam(required = false) Long conversationId,
            @RequestParam(required = false) String message,
            @RequestParam("image") MultipartFile image) {
        byte[] imageBytes;
        try {
            imageBytes = image.getBytes();
        } catch (IOException e) {
            throw new LlmException("Failed to read uploaded image", e);
        }
        ChatResult result = claudeService.chat(
                conversationId, message, imageBytes, image.getContentType(), image.getOriginalFilename());
        return new ChatResponse(result.conversationId(), result.reply());
    }
}