package com.example.banking.auth.api;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
