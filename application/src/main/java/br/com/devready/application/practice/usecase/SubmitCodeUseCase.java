package br.com.devready.application.practice.usecase;

import br.com.devready.application.practice.dto.SubmissionTicket;
import br.com.devready.application.practice.port.ExamLockReader;
import br.com.devready.application.practice.service.ExamGuard;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.practice.service.VisibleLevels;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.SubmissionKind;
import java.util.UUID;

public class SubmitCodeUseCase {

    private final SubmissionDesk desk;
    private final ExamLockReader locks;
    private final VisibleLevels levels;

    public SubmitCodeUseCase(SubmissionDesk desk, ExamLockReader locks, VisibleLevels levels) {
        this.desk = desk;
        this.locks = locks;
        this.levels = levels;
    }

    public SubmissionTicket execute(
            UUID userId, String slug, Language language, String sourceCode) {
        ExamGuard.requireChallengeFree(
                locks, userId, levels.challenge(userId, desk.activeChallenge(slug)).id());
        return desk.queue(userId, slug, language, sourceCode, SubmissionKind.SUBMIT);
    }
}
