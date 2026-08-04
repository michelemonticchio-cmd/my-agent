package com.monticchio.myagent.controller;

import com.monticchio.myagent.dto.AccountDtos.AccountInfo;
import com.monticchio.myagent.entity.User;
import com.monticchio.myagent.service.ConversationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final ConversationService conversationService;

    public AccountController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public AccountInfo account(Authentication authentication) {
        User user = conversationService.getUser(authentication.getName());
        return new AccountInfo(user.getUsername(), user.getCreatedAt());
    }
}
