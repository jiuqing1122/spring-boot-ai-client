package com.example.aiclient.controller;

import com.example.aiclient.dto.LoginRequest;
import com.example.aiclient.dto.RegisterRequest;
import com.example.aiclient.security.LoginUser;
import com.example.aiclient.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req) {
        return ok(authService.register(req));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        return ok(authService.login(req));
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal LoginUser loginUser) {
        return ok(authService.me(loginUser.id()));
    }

    private static Map<String, Object> ok(Object data) {
        return Map.of("code", 200, "message", "success", "data", data);
    }
}