package br.com.devready.infrastructure.gamification.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_achievements")
@IdClass(UserAchievementId.class)
public class UserAchievementEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(length = 50)
    private String slug;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    @Column(name = "session_id")
    private UUID sessionId;

    protected UserAchievementEntity() {}

    public UUID getUserId() {
        return userId;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    public UUID getSessionId() {
        return sessionId;
    }
}
