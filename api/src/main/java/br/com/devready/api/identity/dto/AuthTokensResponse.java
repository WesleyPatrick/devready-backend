package br.com.devready.api.identity.dto;

import br.com.devready.application.identity.dto.AuthTokens;

public record AuthTokensResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserResponse user) {
    public static AuthTokensResponse from(AuthTokens tokens) {
        return new AuthTokensResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresInSeconds(),
                "Bearer",
                UserResponse.from(tokens.user()));
    }
}
