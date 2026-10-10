package br.com.devready.domain.practice;

import br.com.devready.domain.evaluation.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;

public record ChallengeAttempt(
        UUID userId, UUID challengeId, SubmissionStatus status, int xpGained, Instant submittedAt) {

    public ChallengeAttempt {
        if (userId == null || challengeId == null || status == null || submittedAt == null) {
            throw new PracticeException("Envio sem pessoa, desafio, situação ou hora.");
        }
        if (xpGained < 0) {
            throw new PracticeException("Envio com XP negativo.");
        }
        if (xpGained > 0 && status != SubmissionStatus.PASSED) {
            throw new PracticeException("Só envio aceito ganha XP.");
        }
    }

    public boolean counts() {
        return status.finished() && status != SubmissionStatus.ERROR;
    }

    public boolean passed() {
        return status == SubmissionStatus.PASSED;
    }
}
