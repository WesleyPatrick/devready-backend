package br.com.devready.infrastructure.evaluation.judge0;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.evaluation.dto.ExecutionRequest;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.evaluation.CaseResult;
import br.com.devready.domain.evaluation.CaseStatus;
import br.com.devready.domain.evaluation.ExecutionReport;
import br.com.devready.domain.evaluation.HarnessInput;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.evaluation.Verdict;
import br.com.devready.infrastructure.evaluation.SeedChallenges;
import br.com.devready.infrastructure.evaluation.harness.TemplateHarnessBuilder;
import br.com.devready.infrastructure.shared.ThreadSleeper;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("judge0")
class ReferenceSolutionsJudge0Test {

    private static final String MARKER = "#devready-gate-0123456789#";

    private static final Judge0CodeExecutor EXECUTOR =
            new Judge0CodeExecutor(
                    new Judge0Properties(
                            URI.create(
                                    System.getenv()
                                            .getOrDefault("JUDGE0_URL", "http://127.0.0.1:2358")),
                            Duration.ofMillis(500),
                            Duration.ofSeconds(60),
                            Map.of(
                                    Language.JAVA,
                                    new Judge0Properties.LanguageSettings(62, 8_388_608),
                                    Language.PYTHON,
                                    new Judge0Properties.LanguageSettings(71, 4_194_304),
                                    Language.JAVASCRIPT,
                                    new Judge0Properties.LanguageSettings(63, 4_194_304))),
                    new ThreadSleeper());

    private static final List<TestCase> ECHO =
            List.of(
                    new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                    new TestCase(UUID.randomUUID(), 1, "[2]", "2", true));

    private final TemplateHarnessBuilder harness = new TemplateHarnessBuilder();

    static List<SeedChallenges.Reference> references() {
        return SeedChallenges.references();
    }

    private Verdict run(
            Language language,
            String code,
            String function,
            List<TestCase> cases,
            int timeLimitMs,
            String label) {
        long started = System.nanoTime();
        ExecutionReport report =
                EXECUTOR.execute(
                        new ExecutionRequest(
                                language,
                                harness.build(language, code, function, MARKER),
                                HarnessInput.of(cases),
                                timeLimitMs));
        long ms = (System.nanoTime() - started) / 1_000_000;
        Verdict verdict = Verdict.of(report, MARKER, cases, ComparisonMode.EXACT);
        System.out.printf("judge0 | %s | %s | %s | %d ms%n", label, language, verdict.status(), ms);
        return verdict;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("references")
    void everyReferenceSolutionPassesOnTheRealJudge0(SeedChallenges.Reference reference) {
        long started = System.nanoTime();
        ExecutionReport report =
                EXECUTOR.execute(
                        new ExecutionRequest(
                                reference.language(),
                                harness.build(
                                        reference.language(),
                                        reference.code(),
                                        reference.functionName(),
                                        MARKER),
                                HarnessInput.of(reference.cases()),
                                5000));
        long ms = (System.nanoTime() - started) / 1_000_000;
        Verdict verdict = Verdict.of(report, MARKER, reference.cases(), reference.mode());
        System.out.printf(
                "judge0 | %s | %s | %s | %d ms%n",
                reference.slug(), reference.language(), verdict.status(), ms);

        assertThat(verdict.status())
                .as(
                        "%s%nstdout:%n%s%nstderr:%n%s%ncompilação:%n%s%nmensagem: %s",
                        reference,
                        report.stdout(),
                        report.stderr(),
                        report.compileOutput(),
                        report.message())
                .isEqualTo(SubmissionStatus.PASSED);
        assertThat(verdict.results()).hasSize(reference.cases().size());
    }

    @Test
    void aSyntaxErrorInJavaIsACompileError() {
        Verdict verdict =
                run(
                        Language.JAVA,
                        "class Solution {\n    public int f(int x) { return x }\n}\n",
                        "f",
                        ECHO,
                        5000,
                        "compilação");

        assertThat(verdict.status()).isEqualTo(SubmissionStatus.COMPILE_ERROR);
        assertThat(verdict.compileOutput()).contains("error");
    }

    @Test
    void anInfiniteLoopIsATimeLimit() {
        Verdict verdict =
                run(
                        Language.PYTHON,
                        "class Solution:\n    def f(self, x):\n        while True:\n            pass\n",
                        "f",
                        ECHO,
                        2000,
                        "laço infinito");

        assertThat(verdict.status()).isEqualTo(SubmissionStatus.TIME_LIMIT);
        assertThat(verdict.results())
                .extracting(CaseResult::status)
                .containsOnly(CaseStatus.TIME_LIMIT);
    }

    @Test
    void aProgramThatDiesBeforeAnsweringIsARuntimeError() {
        Verdict verdict =
                run(
                        Language.JAVASCRIPT,
                        "throw new Error('quebrou na carga')\nclass Solution {}\n",
                        "f",
                        ECHO,
                        5000,
                        "queda");

        assertThat(verdict.status()).isEqualTo(SubmissionStatus.RUNTIME_ERROR);
        assertThat(verdict.errorMessage()).contains("quebrou na carga");
    }

    @Test
    void printsOfThePersonDoNotBreakTheProtocol() {
        Verdict verdict =
                run(
                        Language.PYTHON,
                        "class Solution:\n    def f(self, x):\n        print('oi')\n        return x\n",
                        "f",
                        ECHO,
                        5000,
                        "print");

        assertThat(verdict.status()).isEqualTo(SubmissionStatus.PASSED);
    }
}
