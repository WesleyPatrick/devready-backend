package br.com.devready.application.practice.usecase;

import br.com.devready.application.evaluation.port.LanguageRuntimes;
import br.com.devready.application.practice.dto.ChallengeLanguageView;
import br.com.devready.application.practice.dto.ChallengeView;
import br.com.devready.application.practice.dto.Placement;
import br.com.devready.application.practice.port.ChallengeAttemptReader;
import br.com.devready.application.practice.port.ExamLockReader;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.practice.service.ExamGuard;
import br.com.devready.application.practice.service.ProblemBoard;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.practice.service.VisibleLevels;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.practice.QuestionStatus;
import br.com.devready.domain.practice.Xp;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class GetChallengeUseCase {

    private final SubmissionDesk desk;
    private final ProblemBoard board;
    private final ChallengeAttemptReader attempts;
    private final SimulationRecordReader simulations;
    private final ExamLockReader locks;
    private final LanguageRuntimes runtimes;
    private final VisibleLevels levels;

    public GetChallengeUseCase(
            SubmissionDesk desk,
            ProblemBoard board,
            ChallengeAttemptReader attempts,
            SimulationRecordReader simulations,
            ExamLockReader locks,
            LanguageRuntimes runtimes,
            VisibleLevels levels) {
        this.desk = desk;
        this.board = board;
        this.attempts = attempts;
        this.simulations = simulations;
        this.locks = locks;
        this.runtimes = runtimes;
        this.levels = levels;
    }

    public ChallengeView execute(UUID userId, String slug) {
        Challenge challenge = levels.challenge(userId, desk.activeChallenge(slug));
        ExamGuard.requireChallengeFree(locks, userId, challenge.id());
        Placement placement = board.placementOf(userId, challenge.tagSlugs());
        QuestionStatus status =
                simulations.challengesSolvedBy(userId).contains(challenge.id())
                        ? QuestionStatus.SOLVED
                        : QuestionStatus.ofAttempts(
                                attempts.findByUser(userId).stream()
                                        .filter(
                                                attempt ->
                                                        attempt.challengeId()
                                                                .equals(challenge.id()))
                                        .toList());
        List<ChallengeLanguageView> languages = ChallengeLanguageView.listOf(challenge, runtimes);
        return new ChallengeView(
                challenge.slug(),
                challenge.title(),
                challenge.difficulty(),
                challenge.statement(),
                challenge.limits(),
                placement.competency(),
                placement.askedBy(),
                status,
                Xp.challengeFirstSolve(challenge.difficulty()),
                languages,
                challenge.visibleTestCases().stream()
                        .sorted(Comparator.comparingInt(TestCase::position))
                        .toList(),
                challenge.hiddenTestCases().size(),
                status == QuestionStatus.SOLVED ? challenge.approachSummary() : null);
    }
}
