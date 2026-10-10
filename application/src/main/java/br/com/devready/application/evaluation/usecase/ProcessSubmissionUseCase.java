package br.com.devready.application.evaluation.usecase;

import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.evaluation.dto.ExecutionRequest;
import br.com.devready.application.evaluation.dto.ProcessOutcome;
import br.com.devready.application.evaluation.exception.ExecutionUnavailableException;
import br.com.devready.application.evaluation.exception.XpAlreadyGrantedException;
import br.com.devready.application.evaluation.port.CodeExecutor;
import br.com.devready.application.evaluation.port.HarnessBuilder;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.application.shared.Sleeper;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.ChallengeLanguage;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.ExecutionReport;
import br.com.devready.domain.evaluation.HarnessInput;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.evaluation.Verdict;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ProcessSubmissionUseCase {

    private static final System.Logger LOG =
            System.getLogger(ProcessSubmissionUseCase.class.getName());

    public static final List<Duration> RETRY_DELAYS =
            List.of(Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofSeconds(30));

    static final String UNAVAILABLE =
            "O executor de código não respondeu. Tente de novo em alguns minutos.";

    static final String FAILURE = "Não foi possível avaliar o envio. Tente de novo.";

    static final String NO_CHALLENGE =
            "O desafio deste envio não está mais disponível nesta linguagem.";

    private final SubmissionRepository submissions;
    private final ChallengeRepository challenges;
    private final HarnessBuilder harness;
    private final CodeExecutor executor;
    private final IdGenerator ids;
    private final Clock clock;
    private final Sleeper sleeper;

    public ProcessSubmissionUseCase(
            SubmissionRepository submissions,
            ChallengeRepository challenges,
            HarnessBuilder harness,
            CodeExecutor executor,
            IdGenerator ids,
            Clock clock,
            Sleeper sleeper) {
        this.submissions = submissions;
        this.challenges = challenges;
        this.harness = harness;
        this.executor = executor;
        this.ids = ids;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    public ProcessOutcome execute(UUID submissionId) {
        CodeSubmission queued = submissions.findById(submissionId).orElse(null);
        if (queued == null || queued.status() != SubmissionStatus.QUEUED) {
            return ProcessOutcome.IGNORED;
        }
        CodeSubmission running = submissions.save(queued.start());
        if (running.status() != SubmissionStatus.RUNNING) {
            return ProcessOutcome.IGNORED;
        }
        try {
            return process(running);
        } catch (XpAlreadyGrantedException e) {
            throw e;
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Falha ao avaliar o envio " + running.id(), e);
            if (stillRunning(running.id())) {
                submissions.save(running.fail(FAILURE, clock.now()));
            }
            return ProcessOutcome.FAILED;
        }
    }

    private ProcessOutcome process(CodeSubmission running) {
        Challenge challenge = challenges.findById(running.challengeId()).orElse(null);
        ChallengeLanguage entry =
                challenge == null ? null : challenge.languages().get(running.language());
        if (entry == null) {
            submissions.save(running.fail(NO_CHALLENGE, clock.now()));
            return ProcessOutcome.FAILED;
        }
        List<TestCase> cases =
                running.kind() == SubmissionKind.RUN
                        ? challenge.visibleTestCases()
                        : challenge.testCases();
        String marker = "#devready-" + ids.next().toString().replace("-", "") + "#";
        ExecutionRequest request =
                new ExecutionRequest(
                        running.language(),
                        harness.build(
                                running.language(),
                                running.sourceCode(),
                                entry.functionName(),
                                marker),
                        HarnessInput.of(cases),
                        challenge.limits().timeLimitMs());
        ExecutionReport report;
        try {
            report = executeWithRetries(request);
        } catch (ExecutionUnavailableException e) {
            LOG.log(
                    System.Logger.Level.WARNING,
                    "Executor fora depois de quatro tentativas no envio "
                            + running.id()
                            + ": "
                            + e.getMessage());
            if (!stillRunning(running.id())) {
                return ProcessOutcome.IGNORED;
            }
            submissions.save(running.fail(UNAVAILABLE, clock.now()));
            return ProcessOutcome.FAILED;
        }
        Verdict verdict = Verdict.of(report, marker, cases, challenge.comparisonMode());
        if (!stillRunning(running.id())) {
            return ProcessOutcome.IGNORED;
        }
        boolean firstSolve =
                verdict.status() == SubmissionStatus.PASSED
                        && running.xpReward() > 0
                        && !submissions.hasXpGained(running.userId(), running.challengeId());
        Instant now = clock.now();
        try {
            submissions.save(running.finish(verdict, firstSolve, now));
        } catch (XpAlreadyGrantedException e) {
            submissions.save(running.finish(verdict, false, now));
        }
        return ProcessOutcome.FINISHED;
    }

    private ExecutionReport executeWithRetries(ExecutionRequest request) {
        for (int attempt = 0; ; attempt++) {
            try {
                return executor.execute(request);
            } catch (ExecutionUnavailableException e) {
                if (attempt >= RETRY_DELAYS.size()) {
                    throw e;
                }
                sleeper.sleep(RETRY_DELAYS.get(attempt));
            }
        }
    }

    private boolean stillRunning(UUID id) {
        return submissions
                .findById(id)
                .map(submission -> submission.status() == SubmissionStatus.RUNNING)
                .orElse(false);
    }
}
