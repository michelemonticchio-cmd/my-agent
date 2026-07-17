package com.monticchio.myagent.controller;

import com.monticchio.myagent.dto.AuthDtos.AuthResponse;
import com.monticchio.myagent.dto.AuthDtos.LoginRequest;
import com.monticchio.myagent.dto.AuthDtos.RegisterRequest;
import com.monticchio.myagent.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        String token = authService.register(request.username(), request.password());
        return new AuthResponse(token);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        return new AuthResponse(token);
    }
}
