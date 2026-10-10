package br.com.devready.infrastructure.catalog.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTagRepository extends JpaRepository<TagEntity, UUID> {
    Optional<TagEntity> findBySlug(String slug);

    List<TagEntity> findBySlugIn(Collection<String> slugs);
}
