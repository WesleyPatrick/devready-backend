package br.com.devready.domain.catalog;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record Challenge(
        UUID id,
        String slug,
        String title,
        UUID areaId,
        Level level,
        Difficulty difficulty,
        String statement,
        String approachSummary,
        ExecutionLimits limits,
        ComparisonMode comparisonMode,
        int maxSubmissions,
        Map<Language, ChallengeLanguage> languages,
        List<TestCase> testCases,
        Set<String> tagSlugs,
        boolean active) {

    public static final int SUBMISSION_CEILING = 5;

    public Challenge {
        Slug.require(slug, "desafio");
        Text.require(title, "título");
        Text.require(statement, "enunciado");
        Text.require(approachSummary, "abordagem esperada");
        if (areaId == null
                || level == null
                || difficulty == null
                || limits == null
                || comparisonMode == null) {
            throw new InvalidChallengeException(
                    "Desafio precisa de área, nível, dificuldade, limites e modo de comparação.");
        }
        if (maxSubmissions < 1 || maxSubmissions > SUBMISSION_CEILING) {
            throw new InvalidChallengeException(
                    "O limite de envios do desafio precisa ficar entre 1 e "
                            + SUBMISSION_CEILING
                            + ".");
        }
        if (languages == null || languages.isEmpty()) {
            throw new InvalidChallengeException("Desafio precisa de ao menos uma linguagem.");
        }
        languages.forEach(
                (key, value) -> {
                    if (value.language() != key) {
                        throw new InvalidChallengeException(
                                "Linguagem " + key + " não bate com a entrada " + value.language());
                    }
                });
        if (testCases == null) {
            throw new InvalidChallengeException("Desafio precisa de casos de teste.");
        }
        long visible = testCases.stream().filter(TestCase::visible).count();
        long hidden = testCases.size() - visible;
        if (visible < 2 || hidden < 2) {
            throw new InvalidChallengeException(
                    "Desafio precisa de ao menos 2 casos visíveis e 2 ocultos.");
        }
        long distinctPositions = testCases.stream().map(TestCase::position).distinct().count();
        if (distinctPositions != testCases.size()) {
            throw new InvalidChallengeException("Casos de teste com posição repetida.");
        }
        languages = Map.copyOf(languages);
        testCases = List.copyOf(testCases);
        tagSlugs = tagSlugs == null ? Set.of() : Set.copyOf(tagSlugs);
    }

    public List<TestCase> visibleTestCases() {
        return testCases.stream().filter(TestCase::visible).toList();
    }

    public List<TestCase> hiddenTestCases() {
        return testCases.stream().filter(tc -> !tc.visible()).toList();
    }
}
