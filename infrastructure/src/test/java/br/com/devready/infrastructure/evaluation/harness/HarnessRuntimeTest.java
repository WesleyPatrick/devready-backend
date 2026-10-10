package br.com.devready.infrastructure.evaluation.harness;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("runtimes")
class HarnessRuntimeTest {

    private static final String MARKER = "#devready-local-0123456789#";

    private static final List<TestCase> ECHO =
            List.of(
                    new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                    new TestCase(UUID.randomUUID(), 1, "[2]", "2", true));

    private final TemplateHarnessBuilder harness = new TemplateHarnessBuilder();

    static List<SeedChallenges.Reference> references() {
        return SeedChallenges.references();
    }

    static List<SeedChallenges.Reference> starters() {
        return SeedChallenges.starters();
    }

    private ExecutionReport run(
            Language language, String code, String function, List<TestCase> cases, Path dir)
            throws Exception {
        String program = harness.build(language, code, function, MARKER);
        return LocalRuntime.run(language, program, HarnessInput.of(cases), dir);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("references")
    void theReferenceSolutionPassesEveryCase(SeedChallenges.Reference reference, @TempDir Path dir)
            throws Exception {
        ExecutionReport report =
                run(
                        reference.language(),
                        reference.code(),
                        reference.functionName(),
                        reference.cases(),
                        dir);

        Verdict verdict = Verdict.of(report, MARKER, reference.cases(), reference.mode());

        assertThat(verdict.status())
                .as(
                        "%s%nstdout:%n%s%nstderr:%n%s%ncompilação:%n%s",
                        reference, report.stdout(), report.stderr(), report.compileOutput())
                .isEqualTo(SubmissionStatus.PASSED);
        assertThat(verdict.results()).hasSize(reference.cases().size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("starters")
    void theStarterCodeCompilesAndAnswersEveryCase(
            SeedChallenges.Reference starter, @TempDir Path dir) throws Exception {
        ExecutionReport report =
                run(
                        starter.language(),
                        starter.code(),
                        starter.functionName(),
                        starter.cases(),
                        dir);

        Verdict verdict = Verdict.of(report, MARKER, starter.cases(), starter.mode());

        assertThat(verdict.status())
                .as("%s%n%s%n%s", starter, report.stderr(), report.compileOutput())
                .isIn(SubmissionStatus.PASSED, SubmissionStatus.FAILED);
        assertThat(verdict.results())
                .hasSize(starter.cases().size())
                .extracting(CaseResult::status)
                .doesNotContain(CaseStatus.ERROR);
    }

    @Test
    void printsOfThePersonGoToStderrInTheThreeLanguages(@TempDir Path dir) throws Exception {
        Map<Language, String> codes =
                Map.of(
                        Language.JAVA,
                        "class Solution {\n"
                                + "    public int f(int x) {\n"
                                + "        System.out.println(\"oi\");\n"
                                + "        return x;\n"
                                + "    }\n"
                                + "}\n",
                        Language.PYTHON,
                        "class Solution:\n    def f(self, x):\n        print('oi')\n        return x\n",
                        Language.JAVASCRIPT,
                        "class Solution {\n"
                                + "  f(x) {\n"
                                + "    console.log('oi')\n"
                                + "    process.stdout.write('oi')\n"
                                + "    return x\n"
                                + "  }\n"
                                + "}\n");
        for (Map.Entry<Language, String> entry : codes.entrySet()) {
            Path own = Files.createDirectories(dir.resolve(entry.getKey().name()));

            ExecutionReport report = run(entry.getKey(), entry.getValue(), "f", ECHO, own);

            assertThat(Verdict.of(report, MARKER, ECHO, ComparisonMode.EXACT).status())
                    .as(entry.getKey() + "\n" + report.stdout() + "\n" + report.stderr())
                    .isEqualTo(SubmissionStatus.PASSED);
            assertThat(report.stderr()).as(entry.getKey().name()).contains("oi");
        }
    }

    @Test
    void anExceptionInOneCaseDoesNotStopTheOthers(@TempDir Path dir) throws Exception {
        List<TestCase> cases =
                List.of(
                        new TestCase(UUID.randomUUID(), 0, "[1]", "0", true),
                        new TestCase(UUID.randomUUID(), 1, "[2]", "10", true));
        Map<Language, String> codes =
                Map.of(
                        Language.JAVA,
                        "class Solution {\n"
                                + "    public int f(int x) {\n"
                                + "        return 10 / (x - 1);\n"
                                + "    }\n"
                                + "}\n",
                        Language.PYTHON,
                        "class Solution:\n    def f(self, x):\n        return 10 // (x - 1)\n",
                        Language.JAVASCRIPT,
                        "class Solution {\n"
                                + "  f(x) {\n"
                                + "    if (x === 1) throw new RangeError('divisão por zero')\n"
                                + "    return 10 / (x - 1)\n"
                                + "  }\n"
                                + "}\n");
        Map<Language, String> errors =
                Map.of(
                        Language.JAVA, "ArithmeticException",
                        Language.PYTHON, "ZeroDivisionError",
                        Language.JAVASCRIPT, "RangeError: divisão por zero");
        for (Map.Entry<Language, String> entry : codes.entrySet()) {
            Path own = Files.createDirectories(dir.resolve(entry.getKey().name()));

            Verdict verdict =
                    Verdict.of(
                            run(entry.getKey(), entry.getValue(), "f", cases, own),
                            MARKER,
                            cases,
                            ComparisonMode.EXACT);

            assertThat(verdict.results())
                    .as(entry.getKey().name())
                    .extracting(CaseResult::status)
                    .containsExactly(CaseStatus.ERROR, CaseStatus.PASSED);
            assertThat(verdict.results().get(0).errorMessage())
                    .contains(errors.get(entry.getKey()));
        }
    }

    @Test
    void aNonFiniteNumberBecomesAnErrorOfTheCaseInsteadOfAnInvalidLine(@TempDir Path dir)
            throws Exception {
        Map<Language, String> codes =
                Map.of(
                        Language.JAVA,
                        "class Solution {\n"
                                + "    public double f(int x) { return 0.0 / 0.0; }\n"
                                + "}\n",
                        Language.PYTHON,
                        "class Solution:\n    def f(self, x):\n        return float('nan')\n");
        for (Map.Entry<Language, String> entry : codes.entrySet()) {
            Path own = Files.createDirectories(dir.resolve(entry.getKey().name()));

            Verdict verdict =
                    Verdict.of(
                            run(entry.getKey(), entry.getValue(), "f", ECHO, own),
                            MARKER,
                            ECHO,
                            ComparisonMode.EXACT);

            assertThat(verdict.results())
                    .as(entry.getKey().name())
                    .extracting(CaseResult::status)
                    .containsOnly(CaseStatus.ERROR);
        }
    }
}
