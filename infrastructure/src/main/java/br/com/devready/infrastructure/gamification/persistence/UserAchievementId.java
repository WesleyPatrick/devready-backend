package br.com.devready.infrastructure.gamification.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserAchievementId implements Serializable {

    private UUID userId;
    private String slug;

    protected UserAchievementId() {}

    public UserAchievementId(UUID userId, String slug) {
        this.userId = userId;
        this.slug = slug;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof UserAchievementId that
                && Objects.equals(userId, that.userId)
                && Objects.equals(slug, that.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, slug);
    }
}
