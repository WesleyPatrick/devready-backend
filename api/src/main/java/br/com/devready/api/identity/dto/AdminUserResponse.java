package br.com.devready.api.identity.dto;

import br.com.devready.domain.identity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String displayName,
        String email,
        String avatarUrl,
        String role,
        boolean active,
        boolean hasPassword,
        List<String> socialProviders,
        Instant createdAt) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.id(),
                user.displayName(),
                user.email(),
                user.avatarUrl(),
                user.role().name(),
                user.active(),
                user.hasPassword(),
                user.socialAccounts().stream().map(a -> a.provider().name()).toList(),
                user.createdAt());
    }
}
