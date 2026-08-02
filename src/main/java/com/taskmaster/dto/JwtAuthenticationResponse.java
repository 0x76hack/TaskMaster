package com.taskmaster.dto;

public record JwtAuthenticationResponse(
    String accessToken,
    String tokenType
) {
    public JwtAuthenticationResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
