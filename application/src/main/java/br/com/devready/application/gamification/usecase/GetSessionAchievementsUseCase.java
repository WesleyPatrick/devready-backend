package br.com.devready.application.gamification.usecase;

import br.com.devready.application.gamification.port.AchievementRepository;
import br.com.devready.domain.gamification.Achievement;
import java.util.List;
import java.util.UUID;

public class GetSessionAchievementsUseCase {

    private final AchievementRepository achievements;

    public GetSessionAchievementsUseCase(AchievementRepository achievements) {
        this.achievements = achievements;
    }

    public List<Achievement> execute(UUID userId, UUID sessionId) {
        return achievements.unlockedBySession(userId, sessionId);
    }
}
