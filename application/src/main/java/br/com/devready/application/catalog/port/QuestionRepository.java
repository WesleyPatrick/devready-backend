package br.com.devready.application.catalog.port;

import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {
    boolean existsBySlug(String slug);

    Optional<Question> findBySlug(String slug);

    Question save(Question question);

    void updateDisplay(UUID id, String title, Difficulty difficulty);

    long countActiveByAreaAndLevel(UUID areaId, Level level);
}
