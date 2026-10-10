package br.com.devready.application.gamification.usecase;

import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.session.usecase.FinalizeSessionUseCase;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.session.SessionStatus;
import java.util.UUID;

public class AfterSubmissionUseCase {

    private final SubmissionRepository submissions;
    private final FinalizeSessionUseCase finalizeSession;
    private final AwardAchievementsUseCase award;

    public AfterSubmissionUseCase(
            SubmissionRepository submissions,
            FinalizeSessionUseCase finalizeSession,
            AwardAchievementsUseCase award) {
        this.submissions = submissions;
        this.finalizeSession = finalizeSession;
        this.award = award;
    }

    public void execute(UUID submissionId) {
        CodeSubmission submission = submissions.findById(submissionId).orElse(null);
        if (submission == null || !submission.status().finished()) {
            return;
        }
        if (submission.sessionId() != null) {
            finalizeSession
                    .execute(submission.sessionId())
                    .filter(session -> session.status() == SessionStatus.COMPLETED)
                    .ifPresent(session -> award.execute(session.userId(), session.id()));
        }
        award.execute(submission.userId());
    }
}
