package com.monticchio.myagent.controller;

import com.monticchio.myagent.entity.User;
import com.monticchio.myagent.service.ConversationService;
import com.monticchio.myagent.service.QuotaService;
import com.monticchio.myagent.service.QuotaService.QuotaStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private final QuotaService quotaService;
    private final ConversationService conversationService;

    public QuotaController(QuotaService quotaService, ConversationService conversationService) {
        this.quotaService = quotaService;
        this.conversationService = conversationService;
    }

    @GetMapping
    public QuotaStatus status(Authentication authentication) {
        User user = conversationService.getUser(authentication.getName());
        return quotaService.getStatus(user);
    }
}
