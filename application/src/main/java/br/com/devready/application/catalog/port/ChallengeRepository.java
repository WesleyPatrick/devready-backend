package br.com.devready.application.catalog.port;

import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeRepository {
    boolean existsBySlug(String slug);

    Optional<Challenge> findBySlug(String slug);

    Challenge save(Challenge challenge);

    Optional<Challenge> findById(UUID id);

    void updateDisplay(UUID id, String title, Difficulty difficulty, int maxSubmissions);

    long countActiveByAreaAndLevel(UUID areaId, Level level);
}
