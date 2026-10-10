package br.com.devready.application.identity.dto;

import java.util.UUID;

public record UpdateProfileCommand(
        UUID userId,
        String displayName,
        String avatarUrl,
        String newPassword,
        String currentPassword) {}
