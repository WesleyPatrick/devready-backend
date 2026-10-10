package br.com.devready.infrastructure.catalog.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAreaRepository extends JpaRepository<AreaEntity, UUID> {
    List<AreaEntity> findByActiveTrueOrderByNameAsc();

    Optional<AreaEntity> findBySlug(String slug);
}
