package br.com.devready.infrastructure.career.persistence;

import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.domain.career.Competency;
import br.com.devready.infrastructure.catalog.persistence.SpringDataTagRepository;
import br.com.devready.infrastructure.catalog.persistence.TagEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaCompetencyRepository implements CompetencyRepository {

    private final SpringDataCompetencyRepository competencies;
    private final SpringDataTagRepository tags;

    public JpaCompetencyRepository(
            SpringDataCompetencyRepository competencies, SpringDataTagRepository tags) {
        this.competencies = competencies;
        this.tags = tags;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Competency> findAll() {
        return competencies.findAll().stream().map(CareerMappers::toDomain).toList();
    }

    @Override
    public Competency save(Competency competency) {
        CompetencyEntity entity =
                competencies
                        .findById(competency.id())
                        .orElseGet(() -> new CompetencyEntity(competency.id()));
        Set<TagEntity> tagEntities = new HashSet<>(tags.findBySlugIn(competency.tagSlugs()));
        return CareerMappers.toDomain(
                competencies.saveAndFlush(CareerMappers.apply(competency, entity, tagEntities)));
    }

    @Override
    public void delete(String slug) {
        competencies
                .findBySlug(slug)
                .ifPresent(
                        entity -> {
                            competencies.delete(entity);
                            competencies.flush();
                        });
    }
}
