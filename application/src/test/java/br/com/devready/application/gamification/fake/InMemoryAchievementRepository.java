package br.com.devready.application.gamification.fake;

import br.com.devready.application.gamification.port.AchievementRepository;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryAchievementRepository implements AchievementRepository {

    private final List<UnlockedAchievement> unlocked = new ArrayList<>();

    @Override
    public List<UnlockedAchievement> findByUser(UUID userId) {
        return unlocked.stream().filter(u -> u.userId().equals(userId)).toList();
    }

    @Override
    public boolean unlock(UnlockedAchievement achievement) {
        boolean exists =
                unlocked.stream()
                        .anyMatch(
                                u ->
                                        u.userId().equals(achievement.userId())
                                                && u.achievement() == achievement.achievement());
        if (exists) {
            return false;
        }
        unlocked.add(achievement);
        return true;
    }

    @Override
    public List<Achievement> unlockedBySession(UUID userId, UUID sessionId) {
        return findByUser(userId).stream()
                .filter(u -> sessionId.equals(u.sessionId()))
                .map(UnlockedAchievement::achievement)
                .sorted()
                .toList();
    }

    public List<UnlockedAchievement> all() {
        return List.copyOf(unlocked);
    }
}
