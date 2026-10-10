package br.com.devready.infrastructure.gamification.persistence;

import br.com.devready.application.gamification.port.AchievementRepository;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaAchievementRepository implements AchievementRepository {

    private final SpringDataUserAchievementRepository achievements;

    public JpaAchievementRepository(SpringDataUserAchievementRepository achievements) {
        this.achievements = achievements;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnlockedAchievement> findByUser(UUID userId) {
        return achievements.findByUserIdOrderByUnlockedAtAscSlugAsc(userId).stream()
                .flatMap(e -> toDomain(e).stream())
                .toList();
    }

    @Override
    public boolean unlock(UnlockedAchievement unlocked) {
        String slug = unlocked.achievement().slug();
        int inserted =
                unlocked.sessionId() == null
                        ? achievements.insertIfAbsent(
                                unlocked.userId(), slug, unlocked.unlockedAt())
                        : achievements.insertIfAbsentForSession(
                                unlocked.userId(),
                                slug,
                                unlocked.unlockedAt(),
                                unlocked.sessionId());
        return inserted == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Achievement> unlockedBySession(UUID userId, UUID sessionId) {
        return achievements.findByUserIdAndSessionId(userId, sessionId).stream()
                .flatMap(e -> Achievement.fromSlug(e.getSlug()).stream())
                .sorted()
                .toList();
    }

    private static Optional<UnlockedAchievement> toDomain(UserAchievementEntity e) {
        return Achievement.fromSlug(e.getSlug())
                .map(
                        a ->
                                new UnlockedAchievement(
                                        e.getUserId(), a, e.getUnlockedAt(), e.getSessionId()));
    }
}
