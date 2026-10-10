package br.com.devready.application.identity.dto;

import br.com.devready.domain.identity.SocialProvider;

public record SocialLoginCommand(
        SocialProvider provider,
        String providerUserId,
        String email,
        String displayName,
        String avatarUrl) {}
