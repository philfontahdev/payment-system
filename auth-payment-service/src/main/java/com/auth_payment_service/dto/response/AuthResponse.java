package com.auth_payment_service.dto.response;

public record AuthResponse(
        String token,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {}
