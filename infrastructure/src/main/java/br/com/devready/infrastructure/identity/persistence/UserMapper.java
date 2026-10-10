package br.com.devready.infrastructure.identity.persistence;

import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.User;
import java.util.List;
import java.util.UUID;

final class UserMapper {

    private UserMapper() {}

    static User toDomain(UserEntity entity) {
        List<SocialAccount> accounts =
                entity.getSocialAccounts().stream()
                        .map(a -> new SocialAccount(a.getProvider(), a.getProviderUserId()))
                        .toList();
        return User.restore(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getAvatarUrl(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.isActive(),
                entity.isMustChangePassword(),
                entity.getCreatedAt(),
                accounts);
    }

    static UserEntity apply(User user, UserEntity entity) {
        entity.setEmail(user.email());
        entity.setDisplayName(user.displayName());
        entity.setAvatarUrl(user.avatarUrl());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(user.role());
        entity.setActive(user.active());
        entity.setMustChangePassword(user.mustChangePassword());
        entity.setCreatedAt(user.createdAt());
        for (SocialAccount account : user.socialAccounts()) {
            boolean alreadyThere =
                    entity.getSocialAccounts().stream()
                            .anyMatch(
                                    a ->
                                            a.getProvider() == account.provider()
                                                    && a.getProviderUserId()
                                                            .equals(account.providerUserId()));
            if (!alreadyThere) {
                entity.getSocialAccounts()
                        .add(
                                new SocialAccountEntity(
                                        UUID.randomUUID(),
                                        entity,
                                        account.provider(),
                                        account.providerUserId()));
            }
        }
        return entity;
    }
}
