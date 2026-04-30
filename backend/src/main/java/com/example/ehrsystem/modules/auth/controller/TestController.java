package com.example.ehrsystem.modules.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        return Map.of(
                "message", "Public endpoint is working",
                "secured", false
        );
    }

    @GetMapping("/secure")
    public Map<String, Object> secureEndpoint(Authentication authentication) {
        return Map.of(
                "message", "Secure endpoint is working",
                "secured", true,
                "authenticatedUser", authentication.getName(),
                "authorities", authentication.getAuthorities()
        );
    }
}