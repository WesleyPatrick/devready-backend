package br.com.devready.domain.session;

import java.util.UUID;

public record SessionChallenge(
        int position, UUID challengeId, int submissionsUsed, UUID bestSubmissionId) {

    public SessionChallenge {
        if (position < 1 || challengeId == null) {
            throw new SessionException("Desafio da sessão sem posição ou desafio.");
        }
        if (submissionsUsed < 0) {
            throw new SessionException("Envios usados negativos: " + submissionsUsed + ".");
        }
    }

    public static SessionChallenge of(int position, UUID challengeId) {
        return new SessionChallenge(position, challengeId, 0, null);
    }

    public int submissionsLeft(int maxSubmissions) {
        return Math.max(0, maxSubmissions - submissionsUsed);
    }

    public SessionChallenge useSubmission(int maxSubmissions) {
        if (submissionsLeft(maxSubmissions) == 0) {
            throw new SessionException("Sem envios restantes no desafio " + position + ".");
        }
        return new SessionChallenge(position, challengeId, submissionsUsed + 1, bestSubmissionId);
    }

    public SessionChallenge withBest(UUID submissionId) {
        return new SessionChallenge(position, challengeId, submissionsUsed, submissionId);
    }
}
