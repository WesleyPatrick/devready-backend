package br.com.devready.application.catalog.fake;

import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryQuestionRepository implements QuestionRepository {
    private final Map<String, Question> questions = new HashMap<>();
    private int saves;

    @Override
    public boolean existsBySlug(String slug) {
        return questions.containsKey(slug);
    }

    @Override
    public Optional<Question> findBySlug(String slug) {
        return Optional.ofNullable(questions.get(slug));
    }

    @Override
    public Question save(Question question) {
        saves++;
        questions.put(question.slug(), question);
        return question;
    }

    @Override
    public void updateDisplay(UUID id, String title, Difficulty difficulty) {
        Question current =
                questions.values().stream()
                        .filter(question -> question.id().equals(id))
                        .findFirst()
                        .orElseThrow();
        questions.put(
                current.slug(),
                new Question(
                        current.id(),
                        current.slug(),
                        title,
                        current.areaId(),
                        current.level(),
                        difficulty,
                        current.statement(),
                        current.explanation(),
                        current.options(),
                        current.tagSlugs(),
                        current.active()));
    }

    @Override
    public long countActiveByAreaAndLevel(UUID areaId, Level level) {
        return questions.values().stream()
                .filter(q -> q.active() && q.areaId().equals(areaId) && q.level() == level)
                .count();
    }

    public Question get(String slug) {
        return questions.get(slug);
    }

    public Collection<Question> all() {
        return questions.values();
    }

    public int saves() {
        return saves;
    }
}
