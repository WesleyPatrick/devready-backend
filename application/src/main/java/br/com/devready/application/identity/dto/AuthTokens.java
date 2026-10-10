package br.com.devready.application.identity.dto;

import br.com.devready.domain.identity.User;

public record AuthTokens(
        String accessToken, String refreshToken, long expiresInSeconds, User user) {}
