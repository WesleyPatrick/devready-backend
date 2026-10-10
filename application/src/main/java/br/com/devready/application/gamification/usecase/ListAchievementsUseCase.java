package br.com.devready.application.gamification.usecase;

import br.com.devready.application.gamification.dto.AchievementView;
import br.com.devready.application.gamification.port.AchievementRepository;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ListAchievementsUseCase {

    private final AchievementRepository achievements;

    public ListAchievementsUseCase(AchievementRepository achievements) {
        this.achievements = achievements;
    }

    public List<AchievementView> execute(UUID userId) {
        Map<Achievement, Instant> unlocked = new EnumMap<>(Achievement.class);
        for (UnlockedAchievement u : achievements.findByUser(userId)) {
            unlocked.put(u.achievement(), u.unlockedAt());
        }
        return Arrays.stream(Achievement.values())
                .map(a -> new AchievementView(a, unlocked.get(a)))
                .toList();
    }
}
