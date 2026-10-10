package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.domain.catalog.Level;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataQuestionRepository extends JpaRepository<QuestionEntity, UUID> {
    boolean existsBySlug(String slug);

    Optional<QuestionEntity> findBySlug(String slug);

    long countByAreaIdAndLevelAndActiveTrue(UUID areaId, Level level);

    List<QuestionEntity> findByAreaIdAndLevelAndActiveTrue(UUID areaId, Level level);

    @Query(
            "select distinct q from QuestionEntity q join q.tags t"
                    + " where q.active = true and q.areaId = :areaId and q.level in :levels"
                    + " and t.slug in :tags")
    List<QuestionEntity> findActiveByAreaWithAnyTag(
            @Param("areaId") UUID areaId,
            @Param("tags") Collection<String> tags,
            @Param("levels") Collection<Level> levels);

    @Query(
            "select distinct q from QuestionEntity q left join fetch q.tags"
                    + " where q.active = true and q.areaId = :areaId and q.level in :levels")
    List<QuestionEntity> findActiveByAreaWithTags(
            @Param("areaId") UUID areaId, @Param("levels") Collection<Level> levels);
}
