package br.com.devready.infrastructure.session.persistence;

import br.com.devready.domain.session.SessionChallenge;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class SessionChallengeEmbeddable {

    @Column(nullable = false)
    private int position;

    @Column(name = "challenge_id", nullable = false)
    private UUID challengeId;

    @Column(name = "submissions_used", nullable = false)
    private int submissionsUsed;

    @Column(name = "best_submission_id")
    private UUID bestSubmissionId;

    protected SessionChallengeEmbeddable() {}

    static SessionChallengeEmbeddable from(SessionChallenge challenge) {
        SessionChallengeEmbeddable e = new SessionChallengeEmbeddable();
        e.position = challenge.position();
        e.challengeId = challenge.challengeId();
        e.submissionsUsed = challenge.submissionsUsed();
        e.bestSubmissionId = challenge.bestSubmissionId();
        return e;
    }

    int position() {
        return position;
    }

    SessionChallenge toDomain() {
        return new SessionChallenge(position, challengeId, submissionsUsed, bestSubmissionId);
    }
}
