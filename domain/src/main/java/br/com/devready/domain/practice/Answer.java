package br.com.devready.domain.practice;

import java.time.Instant;
import java.util.UUID;

public record Answer(
        UUID id,
        UUID userId,
        UUID questionId,
        UUID optionId,
        boolean correct,
        int xpGained,
        Instant answeredAt) {
    public Answer {
        if (id == null
                || userId == null
                || questionId == null
                || optionId == null
                || answeredAt == null) {
            throw new PracticeException("Resposta sem id, pessoa, questão, alternativa ou hora.");
        }
        if (xpGained < 0) {
            throw new PracticeException("Resposta com XP negativo.");
        }
        if (!correct && xpGained > 0) {
            throw new PracticeException("Resposta errada não ganha XP.");
        }
    }
}
