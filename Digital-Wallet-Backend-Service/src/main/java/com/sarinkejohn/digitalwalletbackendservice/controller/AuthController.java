package com.sarinkejohn.digitalwalletbackendservice.controller;

import com.sarinkejohn.digitalwalletbackendservice.dto.AuthResponse;
import com.sarinkejohn.digitalwalletbackendservice.dto.RegisterRequest;
import com.sarinkejohn.digitalwalletbackendservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "Channel", required = false, defaultValue = "WEBP") String channel) {
        request.setChannel(channel);
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody Map<String, String> credentials,
            @RequestHeader(value = "Channel", required = false, defaultValue = "WEBP") String channel) {
        return ResponseEntity.ok(authService.login(
                credentials.get("username"), 
                credentials.get("password"),
                channel));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "Channel", required = false, defaultValue = "WEBP") String channel) {
        request.setChannel(channel);
        return ResponseEntity.ok(authService.registerAdmin(request));
    }
}