package br.com.devready.domain.catalog;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record Question(
        UUID id,
        String slug,
        String title,
        UUID areaId,
        Level level,
        Difficulty difficulty,
        String statement,
        String explanation,
        List<QuestionOption> options,
        Set<String> tagSlugs,
        boolean active) {

    public Question {
        Slug.require(slug, "questão");
        Text.require(title, "título");
        Text.require(statement, "enunciado");
        Text.require(explanation, "explicação");
        if (areaId == null || level == null || difficulty == null) {
            throw new InvalidQuestionException("Questão precisa de área, nível e dificuldade.");
        }
        if (options == null || options.size() < 2 || options.size() > 6) {
            throw new InvalidQuestionException("Questão precisa de 2 a 6 alternativas.");
        }
        long correct = options.stream().filter(QuestionOption::correct).count();
        if (correct != 1) {
            throw new InvalidQuestionException(
                    "Questão precisa de exatamente uma alternativa correta.");
        }
        long distinctPositions = options.stream().map(QuestionOption::position).distinct().count();
        if (distinctPositions != options.size()) {
            throw new InvalidQuestionException("Alternativas com posição repetida.");
        }
        options = List.copyOf(options);
        tagSlugs = tagSlugs == null ? Set.of() : Set.copyOf(tagSlugs);
    }

    public QuestionOption correctOption() {
        return options.stream().filter(QuestionOption::correct).findFirst().orElseThrow();
    }
}
