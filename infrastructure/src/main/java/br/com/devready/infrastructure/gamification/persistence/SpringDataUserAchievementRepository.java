package br.com.devready.infrastructure.gamification.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataUserAchievementRepository
        extends JpaRepository<UserAchievementEntity, UserAchievementId> {

    List<UserAchievementEntity> findByUserIdOrderByUnlockedAtAscSlugAsc(UUID userId);

    List<UserAchievementEntity> findByUserIdAndSessionId(UUID userId, UUID sessionId);

    @Modifying
    @Query(
            value =
                    "insert into user_achievements (user_id, slug, unlocked_at)"
                            + " values (:userId, :slug, :unlockedAt) on conflict do nothing",
            nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") UUID userId,
            @Param("slug") String slug,
            @Param("unlockedAt") Instant unlockedAt);

    @Modifying
    @Query(
            value =
                    "insert into user_achievements (user_id, slug, unlocked_at, session_id)"
                            + " values (:userId, :slug, :unlockedAt, :sessionId)"
                            + " on conflict do nothing",
            nativeQuery = true)
    int insertIfAbsentForSession(
            @Param("userId") UUID userId,
            @Param("slug") String slug,
            @Param("unlockedAt") Instant unlockedAt,
            @Param("sessionId") UUID sessionId);
}
