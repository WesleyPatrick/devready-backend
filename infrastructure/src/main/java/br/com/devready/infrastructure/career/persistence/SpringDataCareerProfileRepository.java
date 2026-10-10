package br.com.devready.infrastructure.career.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCareerProfileRepository
        extends JpaRepository<CareerProfileEntity, UUID> {}
