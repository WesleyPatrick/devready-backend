package br.com.devready.infrastructure.career.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCompetencyRepository extends JpaRepository<CompetencyEntity, UUID> {
    Optional<CompetencyEntity> findBySlug(String slug);

    List<CompetencyEntity> findBySlugIn(Collection<String> slugs);
}
