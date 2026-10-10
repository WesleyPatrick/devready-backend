package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaQuestionRepository implements QuestionRepository {

    private final SpringDataQuestionRepository questions;
    private final SpringDataTagRepository tags;

    public JpaQuestionRepository(
            SpringDataQuestionRepository questions, SpringDataTagRepository tags) {
        this.questions = questions;
        this.tags = tags;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return questions.existsBySlug(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Question> findBySlug(String slug) {
        return questions.findBySlug(slug).map(CatalogMappers::toDomain);
    }

    @Override
    public Question save(Question question) {
        QuestionEntity entity =
                questions
                        .findById(question.id())
                        .orElseGet(() -> new QuestionEntity(question.id()));
        if (!entity.getOptions().isEmpty()) {
            entity.getOptions().clear();
            questions.saveAndFlush(entity);
        }
        var tagEntities = new HashSet<>(tags.findBySlugIn(question.tagSlugs()));
        return CatalogMappers.toDomain(
                questions.saveAndFlush(CatalogMappers.apply(question, entity, tagEntities)));
    }

    @Override
    public void updateDisplay(UUID id, String title, Difficulty difficulty) {
        QuestionEntity entity = questions.findById(id).orElseThrow();
        entity.setTitle(title);
        entity.setDifficulty(difficulty);
        questions.saveAndFlush(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByAreaAndLevel(UUID areaId, Level level) {
        return questions.countByAreaIdAndLevelAndActiveTrue(areaId, level);
    }
}
