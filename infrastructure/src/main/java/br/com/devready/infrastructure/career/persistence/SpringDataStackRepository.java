package br.com.devready.infrastructure.career.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStackRepository extends JpaRepository<StackEntity, UUID> {
    Optional<StackEntity> findBySlug(String slug);
}
