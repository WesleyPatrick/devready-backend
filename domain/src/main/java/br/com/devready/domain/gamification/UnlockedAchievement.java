package br.com.devready.domain.gamification;

import java.time.Instant;
import java.util.UUID;

public record UnlockedAchievement(
        UUID userId, Achievement achievement, Instant unlockedAt, UUID sessionId) {
    public UnlockedAchievement {
        if (userId == null || achievement == null || unlockedAt == null) {
            throw new GamificationException(
                    "Conquista desbloqueada sem pessoa, conquista ou hora.");
        }
    }
}
