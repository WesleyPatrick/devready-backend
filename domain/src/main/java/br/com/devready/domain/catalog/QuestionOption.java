package br.com.devready.domain.catalog;

import java.util.UUID;

public record QuestionOption(UUID id, String text, boolean correct, int position) {
    public QuestionOption {
        if (text == null || text.isBlank()) {
            throw new InvalidQuestionException("Alternativa sem texto.");
        }
        if (position < 0) {
            throw new InvalidQuestionException("Posição negativa em alternativa.");
        }
    }
}
