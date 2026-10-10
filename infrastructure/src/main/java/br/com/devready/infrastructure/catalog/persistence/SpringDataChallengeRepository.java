package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.domain.catalog.Level;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataChallengeRepository extends JpaRepository<ChallengeEntity, UUID> {
    boolean existsBySlug(String slug);

    Optional<ChallengeEntity> findBySlug(String slug);

    long countByAreaIdAndLevelAndActiveTrue(UUID areaId, Level level);

    List<ChallengeEntity> findByAreaIdAndLevelAndActiveTrue(UUID areaId, Level level);

    @Query(
            "select distinct c from ChallengeEntity c join c.tags t"
                    + " where c.active = true and c.areaId = :areaId and c.level in :levels"
                    + " and t.slug in :tags")
    List<ChallengeEntity> findActiveByAreaWithAnyTag(
            @Param("areaId") UUID areaId,
            @Param("tags") Collection<String> tags,
            @Param("levels") Collection<Level> levels);

    @Query(
            "select distinct c from ChallengeEntity c left join fetch c.tags"
                    + " where c.active = true and c.areaId = :areaId and c.level in :levels")
    List<ChallengeEntity> findActiveByAreaWithTags(
            @Param("areaId") UUID areaId, @Param("levels") Collection<Level> levels);
}
