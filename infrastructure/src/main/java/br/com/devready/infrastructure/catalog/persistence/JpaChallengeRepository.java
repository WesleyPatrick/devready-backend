package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaChallengeRepository implements ChallengeRepository {

    private final SpringDataChallengeRepository challenges;
    private final SpringDataTagRepository tags;

    public JpaChallengeRepository(
            SpringDataChallengeRepository challenges, SpringDataTagRepository tags) {
        this.challenges = challenges;
        this.tags = tags;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return challenges.existsBySlug(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Challenge> findBySlug(String slug) {
        return challenges.findBySlug(slug).map(CatalogMappers::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Challenge> findById(UUID id) {
        return challenges.findById(id).map(CatalogMappers::toDomain);
    }

    @Override
    public Challenge save(Challenge challenge) {
        ChallengeEntity entity =
                challenges
                        .findById(challenge.id())
                        .orElseGet(() -> new ChallengeEntity(challenge.id()));
        if (!entity.getLanguages().isEmpty() || !entity.getTestCases().isEmpty()) {
            entity.getLanguages().clear();
            entity.getTestCases().clear();
            challenges.saveAndFlush(entity);
        }
        var tagEntities = new HashSet<>(tags.findBySlugIn(challenge.tagSlugs()));
        return CatalogMappers.toDomain(
                challenges.saveAndFlush(CatalogMappers.apply(challenge, entity, tagEntities)));
    }

    @Override
    public void updateDisplay(UUID id, String title, Difficulty difficulty, int maxSubmissions) {
        ChallengeEntity entity = challenges.findById(id).orElseThrow();
        entity.setTitle(title);
        entity.setDifficulty(difficulty);
        entity.setMaxSubmissions(maxSubmissions);
        challenges.saveAndFlush(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByAreaAndLevel(UUID areaId, Level level) {
        return challenges.countByAreaIdAndLevelAndActiveTrue(areaId, level);
    }
}
