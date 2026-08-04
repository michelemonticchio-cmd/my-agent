package com.monticchio.myagent.controller;

import com.monticchio.myagent.dto.ConversationDtos.ConversationSummary;
import com.monticchio.myagent.dto.ConversationDtos.CreateConversationRequest;
import com.monticchio.myagent.dto.ConversationDtos.MessageDto;
import com.monticchio.myagent.service.ConversationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public List<ConversationSummary> list(Authentication authentication) {
        return conversationService.listNamed(authentication.getName());
    }

    @GetMapping("/general")
    public ConversationSummary general(Authentication authentication) {
        return conversationService.getOrCreateGeneral(authentication.getName());
    }

    @PostMapping
    public ConversationSummary create(@RequestBody CreateConversationRequest request, Authentication authentication) {
        return conversationService.create(authentication.getName(), request.title(), request.plantationLabel());
    }

    @GetMapping("/{id}/messages")
    public List<MessageDto> messages(@PathVariable Long id, Authentication authentication) {
        return conversationService.getMessages(authentication.getName(), id);
    }
}
