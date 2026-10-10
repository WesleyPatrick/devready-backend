package br.com.devready.domain.session;

import java.util.UUID;

public record SessionQuestion(int position, UUID questionId, UUID optionId, Boolean correct) {

    public SessionQuestion {
        if (position < 1 || questionId == null) {
            throw new SessionException("Questão da sessão sem posição ou questão.");
        }
        if ((optionId == null) != (correct == null)) {
            throw new SessionException("Alternativa e acerto andam juntos na questão da sessão.");
        }
    }

    public static SessionQuestion of(int position, UUID questionId) {
        return new SessionQuestion(position, questionId, null, null);
    }

    public boolean answered() {
        return optionId != null;
    }

    public boolean hit() {
        return Boolean.TRUE.equals(correct);
    }

    public SessionQuestion answer(UUID option, boolean right) {
        if (option == null) {
            throw new SessionException("Resposta sem alternativa.");
        }
        return new SessionQuestion(position, questionId, option, right);
    }
}
