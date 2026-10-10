package br.com.devready.application.practice.usecase;

import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.practice.port.ExamLockReader;
import br.com.devready.application.practice.service.ExamGuard;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.practice.service.VisibleLevels;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import java.util.List;
import java.util.UUID;

public class ListChallengeSubmissionsUseCase {

    private final SubmissionDesk desk;
    private final SubmissionRepository submissions;
    private final ExamLockReader locks;
    private final VisibleLevels levels;

    public ListChallengeSubmissionsUseCase(
            SubmissionDesk desk,
            SubmissionRepository submissions,
            ExamLockReader locks,
            VisibleLevels levels) {
        this.desk = desk;
        this.submissions = submissions;
        this.locks = locks;
        this.levels = levels;
    }

    public List<CodeSubmission> execute(UUID userId, String slug) {
        Challenge challenge = levels.challenge(userId, desk.activeChallenge(slug));
        ExamGuard.requireChallengeFree(locks, userId, challenge.id());
        return submissions
                .findByUserAndChallenge(userId, challenge.id(), SubmissionKind.SUBMIT)
                .stream()
                .map(desk::current)
                .toList();
    }
}
