package br.com.devready.application.career.fake;

import br.com.devready.application.career.port.QuestionProgressReader;
import br.com.devready.domain.career.QuestionProgress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryQuestionProgressReader implements QuestionProgressReader {
    private final Map<UUID, Map<String, QuestionProgress>> byUser = new HashMap<>();

    @Override
    public Map<String, QuestionProgress> answeredBy(UUID userId) {
        return Map.copyOf(byUser.getOrDefault(userId, Map.of()));
    }

    public void record(UUID userId, String questionSlug, QuestionProgress progress) {
        byUser.computeIfAbsent(userId, id -> new HashMap<>()).put(questionSlug, progress);
    }
}
