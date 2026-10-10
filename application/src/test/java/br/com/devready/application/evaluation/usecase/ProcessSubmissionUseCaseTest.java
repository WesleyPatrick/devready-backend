package br.com.devready.application.evaluation.usecase;

import static br.com.devready.application.evaluation.fake.SubmissionFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.catalog.fake.InMemoryChallengeRepository;
import br.com.devready.application.evaluation.dto.ExecutionRequest;
import br.com.devready.application.evaluation.dto.ProcessOutcome;
import br.com.devready.application.evaluation.fake.FakeHarnessBuilder;
import br.com.devready.application.evaluation.fake.InMemorySubmissionRepository;
import br.com.devready.application.evaluation.fake.RecordingSleeper;
import br.com.devready.application.evaluation.fake.ScriptedCodeExecutor;
import br.com.devready.application.evaluation.fake.SubmissionFixtures;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CaseResult;
import br.com.devready.domain.evaluation.CaseStatus;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.ExecutionOutcome;
import br.com.devready.domain.evaluation.ExecutionReport;
import br.com.devready.domain.evaluation.HarnessInput;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessSubmissionUseCaseTest {

    private final InMemorySubmissionRepository submissions = new InMemorySubmissionRepository();
    private final InMemoryChallengeRepository challenges = new InMemoryChallengeRepository();
    private final FakeHarnessBuilder harness = new FakeHarnessBuilder();
    private final ScriptedCodeExecutor executor = new ScriptedCodeExecutor();
    private final RecordingSleeper sleeper = new RecordingSleeper();
    private final FixedClock clock = new FixedClock(T0.plusSeconds(5));
    private final ProcessSubmissionUseCase process =
            new ProcessSubmissionUseCase(
                    submissions,
                    challenges,
                    harness,
                    executor,
                    new SequentialIdGenerator(),
                    clock,
                    sleeper);
    private final UUID user = UUID.randomUUID();
    private final Challenge challenge =
            challenges.save(
                    CatalogFixtures.challenge(
                            "soma",
                            "Soma",
                            CatalogFixtures.area("backend"),
                            Difficulty.EASY,
                            "arrays"));

    private CodeSubmission queue(SubmissionKind kind) {
        return submissions.save(SubmissionFixtures.queued(user, challenge, kind, 40, T0));
    }

    private CodeSubmission stored(CodeSubmission submission) {
        return submissions.findById(submission.id()).orElseThrow();
    }

    @Test
    void anAcceptedSubmissionRunsEveryCaseAndGainsTheXpTheFirstTime() {
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        ProcessOutcome outcome = process.execute(queued.id());

        CodeSubmission done = stored(queued);
        assertThat(outcome).isEqualTo(ProcessOutcome.FINISHED);
        assertThat(done.status()).isEqualTo(SubmissionStatus.PASSED);
        assertThat(done.passedCount()).isEqualTo(4);
        assertThat(done.totalCount()).isEqualTo(4);
        assertThat(done.xpGained()).isEqualTo(40);
        assertThat(done.finishedAt()).isEqualTo(T0.plusSeconds(5));
        assertThat(done.results())
                .extracting(CaseResult::visible)
                .containsExactly(true, true, false, false);
        assertThat(submissions.saves())
                .extracting(CodeSubmission::status)
                .containsExactly(
                        SubmissionStatus.QUEUED, SubmissionStatus.RUNNING, SubmissionStatus.PASSED);
        ExecutionRequest request = executor.calls().get(0);
        assertThat(request.language()).isEqualTo(Language.JAVA);
        assertThat(request.timeLimitMs()).isEqualTo(5000);
        assertThat(request.stdin()).isEqualTo(HarnessInput.of(challenge.testCases()));
        assertThat(ScriptedCodeExecutor.markerOf(request))
                .isEqualTo("#devready-00000000000000000000000000000001#");
        assertThat(harness.functions()).containsExactly("f");
    }

    @Test
    void theSecondAcceptedSubmissionGainsNothing() {
        submissions.save(
                SubmissionFixtures.finished(user, challenge.id(), SubmissionStatus.PASSED, 40, T0));
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        process.execute(queued.id());

        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.PASSED);
        assertThat(stored(queued).xpGained()).isZero();
        assertThat(submissions.rejectedSaves()).isZero();
    }

    @Test
    void aRunExecutesOnlyTheVisibleCasesAndIsWorthNoXp() {
        CodeSubmission queued = queue(SubmissionKind.RUN);

        process.execute(queued.id());

        assertThat(executor.calls().get(0).stdin())
                .isEqualTo(HarnessInput.of(challenge.visibleTestCases()));
        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.PASSED);
        assertThat(stored(queued).totalCount()).isEqualTo(2);
        assertThat(stored(queued).xpGained()).isZero();
    }

    @Test
    void aWrongAnswerFailsAndKeepsTheActualOutput() {
        executor.answers(args -> ((BigDecimal) args.get(0)).intValue() == 2 ? 99 : args.get(0));
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        process.execute(queued.id());

        CodeSubmission done = stored(queued);
        assertThat(done.status()).isEqualTo(SubmissionStatus.FAILED);
        assertThat(done.passedCount()).isEqualTo(3);
        assertThat(done.xpGained()).isZero();
        assertThat(done.results().get(1).status()).isEqualTo(CaseStatus.FAILED);
        assertThat(done.results().get(1).actualJson()).isEqualTo("99");
    }

    @Test
    void anExceptionInACaseIsAnErrorOfThatCase() {
        executor.answers(
                args -> {
                    if (((BigDecimal) args.get(0)).intValue() == 3) {
                        throw new IllegalStateException("IndexError: list index out of range");
                    }
                    return args.get(0);
                });
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        process.execute(queued.id());

        assertThat(stored(queued).results().get(2).status()).isEqualTo(CaseStatus.ERROR);
        assertThat(stored(queued).results().get(2).errorMessage())
                .isEqualTo("IndexError: list index out of range");
    }

    @Test
    void aCompileErrorIsStoredWithTheCompilerOutput() {
        executor.thenReport(
                new ExecutionReport(
                        ExecutionOutcome.COMPILE_ERROR, "", "", "Main.java:2: error", null));
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        process.execute(queued.id());

        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.COMPILE_ERROR);
        assertThat(stored(queued).compileOutput()).isEqualTo("Main.java:2: error");
        assertThat(stored(queued).results()).isEmpty();
    }

    @Test
    void aCrashWithoutAnyAnswerIsARuntimeError() {
        executor.thenReport(
                new ExecutionReport(ExecutionOutcome.CRASHED, "", "SyntaxError", null, null));
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        process.execute(queued.id());

        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.RUNTIME_ERROR);
        assertThat(stored(queued).errorMessage()).isEqualTo("SyntaxError");
    }

    @Test
    void aTimeLimitKeepsTheCasesThatAnswered() {
        executor.thenTimeLimitAfter(2);
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        process.execute(queued.id());

        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.TIME_LIMIT);
        assertThat(stored(queued).passedCount()).isEqualTo(2);
        assertThat(stored(queued).results())
                .extracting(CaseResult::status)
                .containsExactly(
                        CaseStatus.PASSED,
                        CaseStatus.PASSED,
                        CaseStatus.TIME_LIMIT,
                        CaseStatus.TIME_LIMIT);
    }

    @Test
    void aRepeatedMessageOrAnUnknownSubmissionIsIgnored() {
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        process.execute(queued.id());

        assertThat(process.execute(queued.id())).isEqualTo(ProcessOutcome.IGNORED);
        assertThat(process.execute(UUID.randomUUID())).isEqualTo(ProcessOutcome.IGNORED);
        assertThat(executor.calls()).hasSize(1);
    }

    @Test
    void theExecutorIsTriedFourTimesAndThenTheSubmissionIsAnError() {
        executor.thenUnavailable(4);
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        ProcessOutcome outcome = process.execute(queued.id());

        assertThat(outcome).isEqualTo(ProcessOutcome.FAILED);
        assertThat(executor.calls()).hasSize(4);
        assertThat(sleeper.slept())
                .containsExactly(
                        Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofSeconds(30));
        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.ERROR);
        assertThat(stored(queued).errorMessage())
                .isEqualTo("O executor de código não respondeu. Tente de novo em alguns minutos.");
    }

    @Test
    void anExecutorThatComesBackOnTheSecondTryFinishesTheSubmission() {
        executor.thenUnavailable(1);
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        assertThat(process.execute(queued.id())).isEqualTo(ProcessOutcome.FINISHED);
        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.PASSED);
        assertThat(sleeper.slept()).containsExactly(Duration.ofSeconds(2));
    }

    @Test
    void aSubmissionForAChallengeThatNoLongerExistsIsAnError() {
        CodeSubmission queued =
                submissions.save(
                        CodeSubmission.queue(
                                UUID.randomUUID(),
                                user,
                                UUID.randomUUID(),
                                Language.PYTHON,
                                "x",
                                SubmissionKind.SUBMIT,
                                4,
                                40,
                                T0));

        assertThat(process.execute(queued.id())).isEqualTo(ProcessOutcome.FAILED);
        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.ERROR);
        assertThat(executor.calls()).isEmpty();
    }

    @Test
    void aSubmissionInALanguageTheChallengeNoLongerHasIsAnError() {
        Challenge javaOnly =
                new Challenge(
                        UUID.randomUUID(),
                        "so-java",
                        "So Java",
                        challenge.areaId(),
                        challenge.level(),
                        challenge.difficulty(),
                        challenge.statement(),
                        challenge.approachSummary(),
                        challenge.limits(),
                        challenge.comparisonMode(),
                        challenge.maxSubmissions(),
                        Map.of(Language.JAVA, challenge.languages().get(Language.JAVA)),
                        challenge.testCases(),
                        challenge.tagSlugs(),
                        true);
        challenges.save(javaOnly);
        CodeSubmission queued =
                submissions.save(
                        CodeSubmission.queue(
                                UUID.randomUUID(),
                                user,
                                javaOnly.id(),
                                Language.PYTHON,
                                "x",
                                SubmissionKind.SUBMIT,
                                4,
                                40,
                                T0));

        assertThat(process.execute(queued.id())).isEqualTo(ProcessOutcome.FAILED);
        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.ERROR);
        assertThat(executor.calls()).isEmpty();
    }

    @Test
    void anUnexpectedFailureWhileEvaluatingMakesTheSubmissionAnErrorWithTheCause() {
        ProcessSubmissionUseCase failing =
                new ProcessSubmissionUseCase(
                        submissions,
                        challenges,
                        (language, code, function, marker) -> {
                            throw new IllegalStateException("Função inválida: f()");
                        },
                        executor,
                        new SequentialIdGenerator(),
                        clock,
                        sleeper);
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);

        assertThat(failing.execute(queued.id())).isEqualTo(ProcessOutcome.FAILED);
        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.ERROR);
        assertThat(stored(queued).errorMessage())
                .isEqualTo("Não foi possível avaliar o envio. Tente de novo.");
        assertThat(executor.calls()).isEmpty();
    }

    @Test
    void whenTheRaceForTheXpIsLostTheSubmissionStaysAcceptedWithoutXp() {
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);
        submissions.beforeNextFinish(
                () ->
                        submissions.save(
                                SubmissionFixtures.finished(
                                        user, challenge.id(), SubmissionStatus.PASSED, 40, T0)));

        assertThat(process.execute(queued.id())).isEqualTo(ProcessOutcome.FINISHED);
        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.PASSED);
        assertThat(stored(queued).xpGained()).isZero();
        assertThat(submissions.rejectedSaves()).isEqualTo(1);
    }

    @Test
    void aSubmissionThatExpiredWhileRunningStaysAnError() {
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);
        executor.during(
                () -> submissions.save(stored(queued).expireAt(T0.plus(Duration.ofMinutes(3)))));

        assertThat(process.execute(queued.id())).isEqualTo(ProcessOutcome.IGNORED);
        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.ERROR);
        assertThat(stored(queued).errorMessage())
                .isEqualTo("O envio não terminou a tempo. Tente de novo.");
    }

    @Test
    void aSubmissionThatExpiredBeforeStartingIsIgnoredWithoutRunningAnything() {
        CodeSubmission queued = queue(SubmissionKind.SUBMIT);
        submissions.beforeNextStart(
                () -> submissions.save(stored(queued).expireAt(T0.plus(Duration.ofMinutes(3)))));

        assertThat(process.execute(queued.id())).isEqualTo(ProcessOutcome.IGNORED);
        assertThat(executor.calls()).isEmpty();
        assertThat(harness.functions()).isEmpty();
        assertThat(stored(queued).status()).isEqualTo(SubmissionStatus.ERROR);
    }

    @Test
    void retryDelaysAreTwoTenAndThirtySeconds() {
        assertThat(ProcessSubmissionUseCase.RETRY_DELAYS)
                .containsExactly(
                        Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofSeconds(30));
    }
}
