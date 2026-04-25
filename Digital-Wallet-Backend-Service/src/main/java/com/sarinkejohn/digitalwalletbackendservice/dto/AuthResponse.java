package com.sarinkejohn.digitalwalletbackendservice.dto;

public record AuthResponse(String token, String username, String role, Long userId) {
}