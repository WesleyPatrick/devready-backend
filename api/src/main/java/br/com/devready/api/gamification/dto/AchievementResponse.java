package br.com.devready.api.gamification.dto;

import br.com.devready.application.gamification.dto.AchievementView;
import java.time.Instant;

public record AchievementResponse(
        String slug, String name, String description, String icon, Instant unlockedAt) {

    public static AchievementResponse from(AchievementView view) {
        return new AchievementResponse(
                view.achievement().slug(),
                view.achievement().displayName(),
                view.achievement().description(),
                view.achievement().icon(),
                view.unlockedAt());
    }
}
