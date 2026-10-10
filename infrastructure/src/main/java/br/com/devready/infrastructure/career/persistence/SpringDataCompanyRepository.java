package br.com.devready.infrastructure.career.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCompanyRepository extends JpaRepository<CompanyEntity, UUID> {
    List<CompanyEntity> findByActiveTrue();

    Optional<CompanyEntity> findBySlug(String slug);

    List<CompanyEntity> findBySlugIn(Collection<String> slugs);
}
