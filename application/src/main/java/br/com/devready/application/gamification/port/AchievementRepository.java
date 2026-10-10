package br.com.devready.application.gamification.port;

import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import java.util.List;
import java.util.UUID;

public interface AchievementRepository {

    List<UnlockedAchievement> findByUser(UUID userId);

    boolean unlock(UnlockedAchievement unlocked);

    List<Achievement> unlockedBySession(UUID userId, UUID sessionId);
}
