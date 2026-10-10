package br.com.devready.application.practice.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.practice.dto.ProblemPage;
import br.com.devready.application.practice.dto.ProblemQuery;
import br.com.devready.application.practice.dto.ProblemView;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.ProblemType;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.practice.QuestionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ListProblemsUseCaseTest {

    private static final Instant NOW = Instant.ofEpochSecond(1_700_000_000L);

    private final PracticeWorld world = new PracticeWorld();
    private final ListProblemsUseCase listProblems =
            new ListProblemsUseCase(
                    world.board, world.answers, world.attempts, world.simulations, world.examLocks);

    private ProblemPage page(int page, int size) {
        return listProblems.execute(
                new ProblemQuery(world.user, page, size, null, null, null, null, null, null));
    }

    private ProblemPage filtered(
            ProblemType type,
            QuestionStatus status,
            Difficulty difficulty,
            String competency,
            String company) {
        return listProblems.execute(
                new ProblemQuery(
                        world.user, 1, 50, type, status, difficulty, competency, company, null));
    }

    private static List<String> slugs(ProblemPage page) {
        return page.items().stream().map(item -> item.problem().slug()).toList();
    }

    private Map<String, ProblemView> bySlug() {
        return page(1, 50).items().stream()
                .collect(Collectors.toMap(item -> item.problem().slug(), Function.identity()));
    }

    @Test
    void needsAGoal() {
        assertThatThrownBy(() -> page(1, 10)).isInstanceOf(CareerProfileNotFoundException.class);
    }

    @Test
    void listsTheProblemsOfTheStackAreaInTheOrderOfTheTrack() {
        world.withGoal();

        ProblemPage page = page(1, 50);

        assertThat(slugs(page))
                .containsExactly(
                        "q-sql-easy",
                        "c-sql",
                        "q-java-sql",
                        "q-sql-hard",
                        "q-java",
                        "q-http",
                        "q-none",
                        "q-seg",
                        "q-cripto");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.totalItems()).isEqualTo(9);
    }

    @Test
    void eachProblemTakesTheFirstCompetencyOfTheTrackWithACommonTagAndItsCompanies() {
        world.withGoal();

        Map<String, ProblemView> items = bySlug();

        assertThat(items.get("q-java-sql").competency().slug()).isEqualTo("sql");
        assertThat(items.get("q-java-sql").askedBy()).containsExactly("nubank", "google");
        assertThat(items.get("q-java").askedBy()).containsExactly("nubank");
        assertThat(items.get("q-http").competency().slug()).isEqualTo("apis-rest");
        assertThat(items.get("q-http").askedBy()).containsExactly("google");
        assertThat(items.get("c-sql").problem().type()).isEqualTo(ProblemType.CHALLENGE);
        assertThat(items.get("c-sql").competency().slug()).isEqualTo("sql");
    }

    @Test
    void outsideTheTrackTheCompetencyComesFromTheBaseAndNobodyAsksForIt() {
        world.withGoal();

        Map<String, ProblemView> items = bySlug();

        assertThat(items.get("q-seg").competency().slug()).isEqualTo("autenticacao");
        assertThat(items.get("q-cripto").competency().slug()).isEqualTo("criptografia");
        assertThat(items.get("q-seg").askedBy()).isEmpty();
        assertThat(items.get("q-none").competency()).isNull();
        assertThat(items.get("q-none").askedBy()).isEmpty();
    }

    @Test
    void aChallengeTakesItsStatusAndRateFromTheSubmissions() {
        world.withGoal();
        assertThat(bySlug().get("c-sql").status()).isEqualTo(QuestionStatus.NOT_STARTED);
        assertThat(bySlug().get("c-sql").acceptance()).isNull();

        world.submitted(world.user, "c-sql", SubmissionStatus.FAILED, 0, NOW);
        assertThat(bySlug().get("c-sql").status()).isEqualTo(QuestionStatus.ATTEMPTED);
        world.submitted(world.user, "c-sql", SubmissionStatus.PASSED, 40, NOW.plusSeconds(1));
        assertThat(bySlug().get("c-sql").status()).isEqualTo(QuestionStatus.SOLVED);

        for (int i = 0; i < 3; i++) {
            world.submitted(UUID.randomUUID(), "c-sql", SubmissionStatus.PASSED, 40, NOW);
        }
        world.submitted(UUID.randomUUID(), "c-sql", SubmissionStatus.COMPILE_ERROR, 0, NOW);
        world.submitted(UUID.randomUUID(), "c-sql", SubmissionStatus.ERROR, 0, NOW);

        assertThat(bySlug().get("c-sql").acceptance()).isEqualTo(0.6);
    }

    @Test
    void titlesAreOrderedIgnoringCaseAndAccentsThenBySlug() {
        world.withGoal();
        Area backend = world.areas.findBySlug("backend").orElseThrow();
        for (String[] extra :
                List.of(
                        new String[] {"q-join-b", "Junção"},
                        new String[] {"q-join-a", "Junção"},
                        new String[] {"q-index", "Índice ou id como key"},
                        new String[] {"q-limit", "limite de páginas"},
                        new String[] {"q-hash", "Hash de índice"})) {
            world.questions.save(
                    CatalogFixtures.question(
                            extra[0], extra[1], backend, Difficulty.EASY, true, "sql"));
        }

        ProblemPage sql = filtered(null, null, Difficulty.EASY, "sql", null);

        assertThat(slugs(sql))
                .containsExactly(
                        "q-sql-easy",
                        "q-hash",
                        "q-index",
                        "q-join-a",
                        "q-join-b",
                        "q-limit",
                        "c-sql");
    }

    @Test
    void theStatusComesFromTheAnswersOfThePerson() {
        world.withGoal();
        world.answered(world.user, "q-sql-easy", false, 0, NOW);
        world.answered(world.user, "q-sql-easy", true, 20, NOW.plusSeconds(60));
        world.answered(world.user, "q-java", false, 0, NOW);
        world.answered(UUID.randomUUID(), "q-http", true, 20, NOW);

        Map<String, ProblemView> items = bySlug();

        assertThat(items.get("q-sql-easy").status()).isEqualTo(QuestionStatus.SOLVED);
        assertThat(items.get("q-java").status()).isEqualTo(QuestionStatus.ATTEMPTED);
        assertThat(items.get("q-http").status()).isEqualTo(QuestionStatus.NOT_STARTED);
    }

    @Test
    void filtersByTypeStatusDifficultyCompetencyAndCompany() {
        world.withGoal();
        world.answered(world.user, "q-java", true, 20, NOW);

        assertThat(slugs(filtered(ProblemType.CHALLENGE, null, null, null, null)))
                .containsExactly("c-sql");
        assertThat(slugs(filtered(null, QuestionStatus.SOLVED, null, null, null)))
                .containsExactly("q-java");
        assertThat(
                        slugs(
                                filtered(
                                        ProblemType.QUESTION,
                                        QuestionStatus.NOT_STARTED,
                                        Difficulty.HARD,
                                        null,
                                        null)))
                .containsExactly("q-sql-hard", "q-cripto");
        assertThat(slugs(filtered(null, null, null, "sql", null)))
                .containsExactly("q-sql-easy", "c-sql", "q-java-sql", "q-sql-hard");
        assertThat(slugs(filtered(null, null, null, null, "google")))
                .containsExactly("q-sql-easy", "c-sql", "q-java-sql", "q-sql-hard", "q-http");
        assertThat(filtered(null, null, null, "nao-existe", null).items()).isEmpty();
        assertThat(filtered(null, null, null, " ", "").items()).hasSize(9);
    }

    @Test
    void pagesStartAtOneAndStayInsideTheList() {
        world.withGoal();

        ProblemPage second = page(2, 4);

        assertThat(slugs(second)).containsExactly("q-java", "q-http", "q-none", "q-seg");
        assertThat(second.page()).isEqualTo(2);
        assertThat(second.totalPages()).isEqualTo(3);
        assertThat(second.totalItems()).isEqualTo(9);
        ProblemPage beyond = page(99, 4);
        assertThat(beyond.page()).isEqualTo(3);
        assertThat(slugs(beyond)).containsExactly("q-cripto");
        assertThat(page(0, 4).page()).isEqualTo(1);
        assertThat(page(1, 0).items()).hasSize(1);
        assertThat(page(1, 500).items()).hasSize(9);
    }

    @Test
    void anEmptyResultIsOneEmptyPage() {
        world.withGoal();

        ProblemPage empty = filtered(null, null, null, null, "meta");

        assertThat(empty.items()).isEmpty();
        assertThat(empty.page()).isEqualTo(1);
        assertThat(empty.totalPages()).isEqualTo(1);
        assertThat(empty.totalItems()).isZero();
    }

    @Test
    void theRateNeedsFivePeopleAndIsReadOnlyForTheQuestionsOfThePage() {
        world.withGoal();
        for (int i = 0; i < 3; i++) {
            world.answered(UUID.randomUUID(), "q-sql-easy", true, 20, NOW);
        }
        for (int i = 0; i < 2; i++) {
            world.answered(UUID.randomUUID(), "q-sql-easy", false, 0, NOW);
        }
        for (int i = 0; i < 4; i++) {
            world.answered(UUID.randomUUID(), "q-java", true, 20, NOW);
        }

        Map<String, ProblemView> items = bySlug();

        assertThat(items.get("q-sql-easy").acceptance()).isEqualTo(0.6);
        assertThat(items.get("q-java").acceptance()).isNull();

        page(1, 2);

        assertThat(world.answers.lastQuestionQuery())
                .containsExactly(world.question("q-sql-easy").id());
        assertThat(world.attempts.lastChallengeQuery())
                .containsExactly(world.challenges.get("c-sql").id());
    }

    @Test
    void aChallengeOfAnOpenSimulationIsMarkedInExam() {
        world.withGoal();
        world.examLocks.lock(world.user, world.challenges.get("c-sql").id(), UUID.randomUUID());

        Map<String, ProblemView> items = bySlug();

        assertThat(items.get("c-sql").inExam()).isTrue();
        assertThat(items.get("q-sql-easy").inExam()).isFalse();
        world.examLocks.unlockAll();
        assertThat(bySlug().get("c-sql").inExam()).isFalse();
    }

    @Test
    void aQuestionOfAnOpenSimulationIsMarkedInExam() {
        world.withGoal();
        UUID question = world.question("q-sql-easy").id();
        world.examLocks.lockQuestion(world.user, question, UUID.randomUUID());

        Map<String, ProblemView> items = bySlug();

        assertThat(items.get("q-sql-easy").inExam()).isTrue();
        assertThat(items.get("q-java").inExam()).isFalse();
        assertThat(items.get("c-sql").inExam()).isFalse();
        world.examLocks.unlockAll();
        assertThat(bySlug().get("q-sql-easy").inExam()).isFalse();
    }

    @Test
    void whatThePersonGotRightInACorrectedSimulationIsSolved() {
        world.withGoal();
        world.answered(world.user, "q-java", false, 0, NOW);
        world.simulations.solveQuestion(world.user, world.question("q-java").id());
        world.simulations.solveChallenge(world.user, world.challenges.get("c-sql").id());

        Map<String, ProblemView> items = bySlug();

        assertThat(items.get("q-java").status()).isEqualTo(QuestionStatus.SOLVED);
        assertThat(items.get("c-sql").status()).isEqualTo(QuestionStatus.SOLVED);
        assertThat(items.get("q-http").status()).isEqualTo(QuestionStatus.NOT_STARTED);
        assertThat(
                        filtered(null, QuestionStatus.SOLVED, null, null, null).items().stream()
                                .map(item -> item.problem().slug()))
                .containsExactlyInAnyOrder("q-java", "c-sql");
    }

    @Test
    void submissionsOfASessionDoNotChangeTheStatusNorTheRate() {
        world.withGoal();
        world.submittedInSession(
                world.user, "c-sql", SubmissionStatus.FAILED, 1, UUID.randomUUID(), NOW);
        for (int i = 0; i < 5; i++) {
            world.submittedInSession(
                    UUID.randomUUID(), "c-sql", SubmissionStatus.PASSED, 4, UUID.randomUUID(), NOW);
        }

        assertThat(bySlug().get("c-sql").status()).isEqualTo(QuestionStatus.NOT_STARTED);
        assertThat(bySlug().get("c-sql").acceptance()).isNull();
    }
}
