package br.com.devready.application.practice.usecase;

import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.practice.dto.SubmissionView;
import br.com.devready.application.practice.exception.SubmissionNotFoundException;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.evaluation.CodeSubmission;
import java.util.UUID;

public class GetSubmissionUseCase {

    private final SubmissionRepository submissions;
    private final ChallengeRepository challenges;
    private final SubmissionDesk desk;

    public GetSubmissionUseCase(
            SubmissionRepository submissions, ChallengeRepository challenges, SubmissionDesk desk) {
        this.submissions = submissions;
        this.challenges = challenges;
        this.desk = desk;
    }

    public SubmissionView execute(UUID userId, UUID submissionId) {
        CodeSubmission found =
                submissions
                        .findById(submissionId)
                        .filter(submission -> submission.userId().equals(userId))
                        .orElseThrow(SubmissionNotFoundException::new);
        Challenge challenge =
                challenges
                        .findById(found.challengeId())
                        .orElseThrow(SubmissionNotFoundException::new);
        return SubmissionView.of(desk.current(found), challenge);
    }
}
