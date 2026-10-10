package br.com.devready.api.identity.dto;

import br.com.devready.domain.identity.User;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        String role,
        boolean hasPassword,
        List<String> providers,
        boolean mustChangePassword) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.id(),
                user.email(),
                user.displayName(),
                user.avatarUrl(),
                user.role().name(),
                user.hasPassword(),
                user.socialAccounts().stream().map(a -> a.provider().name()).toList(),
                user.mustChangePassword());
    }
}
