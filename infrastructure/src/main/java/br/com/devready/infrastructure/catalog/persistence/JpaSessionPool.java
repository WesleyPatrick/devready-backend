package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.application.session.port.SessionPool;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaSessionPool implements SessionPool {

    private final SpringDataAreaRepository areas;
    private final SpringDataQuestionRepository questions;
    private final SpringDataChallengeRepository challenges;

    public JpaSessionPool(
            SpringDataAreaRepository areas,
            SpringDataQuestionRepository questions,
            SpringDataChallengeRepository challenges) {
        this.areas = areas;
        this.questions = questions;
        this.challenges = challenges;
    }

    @Override
    public Optional<Area> area(UUID areaId) {
        return areas.findById(areaId).map(CatalogMappers::toDomain);
    }

    @Override
    public List<Question> activeQuestions(UUID areaId, Level level) {
        return questions.findByAreaIdAndLevelAndActiveTrue(areaId, level).stream()
                .map(CatalogMappers::toDomain)
                .toList();
    }

    @Override
    public List<Challenge> activeChallenges(UUID areaId, Level level) {
        return challenges.findByAreaIdAndLevelAndActiveTrue(areaId, level).stream()
                .map(CatalogMappers::toDomain)
                .toList();
    }

    @Override
    public List<Question> activeQuestionsTagged(
            UUID areaId, Set<String> tagSlugs, Set<Level> levels) {
        if (tagSlugs.isEmpty() || levels.isEmpty()) {
            return List.of();
        }
        return questions.findActiveByAreaWithAnyTag(areaId, tagSlugs, levels).stream()
                .map(CatalogMappers::toDomain)
                .toList();
    }

    @Override
    public List<Question> questions(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return questions.findAllById(ids).stream().map(CatalogMappers::toDomain).toList();
    }

    @Override
    public List<Challenge> challenges(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return challenges.findAllById(ids).stream().map(CatalogMappers::toDomain).toList();
    }
}
