package br.com.devready.application.practice.usecase;

import static br.com.devready.application.evaluation.fake.SubmissionFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.devready.application.career.dto.TrackChallengeView;
import br.com.devready.application.career.dto.TrackCompetencyDetail;
import br.com.devready.application.career.dto.TrackQuestionView;
import br.com.devready.application.career.usecase.GetLearningTrackUseCase;
import br.com.devready.application.career.usecase.GetTrackCompetencyUseCase;
import br.com.devready.application.evaluation.fake.FixedLanguageRuntimes;
import br.com.devready.application.evaluation.fake.RecordingSubmissionQueue;
import br.com.devready.application.practice.dto.ProblemPage;
import br.com.devready.application.practice.dto.ProblemQuery;
import br.com.devready.application.practice.exception.ChallengeNotFoundException;
import br.com.devready.application.practice.exception.QuestionNotFoundException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.career.CompetencyStatus;
import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.practice.QuestionStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LevelRuleTest {

    private static final String CODE = "class Solution:\n    pass\n";

    private final PracticeWorld world = new PracticeWorld();
    private final FixedClock clock = new FixedClock(T0);
    private final RecordingSubmissionQueue queue = new RecordingSubmissionQueue();
    private final SubmissionDesk desk =
            new SubmissionDesk(
                    world.challenges, world.submissions, queue, new SequentialIdGenerator(), clock);
    private final ListProblemsUseCase listProblems =
            new ListProblemsUseCase(
                    world.board, world.answers, world.attempts, world.simulations, world.examLocks);
    private final GetQuestionUseCase getQuestion =
            new GetQuestionUseCase(
                    world.questions,
                    world.board,
                    world.answers,
                    world.simulations,
                    world.examLocks,
                    world.levels);
    private final AnswerQuestionUseCase answerQuestion =
            new AnswerQuestionUseCase(
                    world.questions,
                    world.answers,
                    world.examLocks,
                    world.levels,
                    new SequentialIdGenerator(),
                    clock);
    private final GetDailyQuestionUseCase getDaily =
            new GetDailyQuestionUseCase(
                    world.board, world.questions, world.answers, world.simulations, clock);
    private final GetChallengeUseCase getChallenge =
            new GetChallengeUseCase(
                    desk,
                    world.board,
                    world.attempts,
                    world.simulations,
                    world.examLocks,
                    new FixedLanguageRuntimes(),
                    world.levels);
    private final RunCodeUseCase run = new RunCodeUseCase(desk, world.examLocks, world.levels);
    private final SubmitCodeUseCase submit =
            new SubmitCodeUseCase(desk, world.examLocks, world.levels);
    private final GetTrackCompetencyUseCase getCompetency =
            new GetTrackCompetencyUseCase(world.assembler, world.areas, world.catalog);
    private final GetLearningTrackUseCase getTrack = new GetLearningTrackUseCase(world.assembler);
    private final GetProgressUseCase getProgress =
            new GetProgressUseCase(world.answers, world.attempts, world.simulations, clock);

    @BeforeEach
    void aboveJunior() {
        world.withLevels();
    }

    private ProblemPage page(QuestionStatus status, Level level) {
        return listProblems.execute(
                new ProblemQuery(world.user, 1, 50, null, status, null, null, null, level));
    }

    private List<String> listed(Level level) {
        return page(null, level).items().stream().map(item -> item.problem().slug()).toList();
    }

    private List<String> listed() {
        return listed(null);
    }

    private CompetencyStatus statusOf(String competency) {
        return getTrack.execute(world.user).competencies().stream()
                .filter(item -> item.competency().slug().equals(competency))
                .findFirst()
                .orElseThrow()
                .status();
    }

    @Test
    void theListShowsTheLevelsUpToTheGoalAndCountsOnlyThem() {
        world.withGoal(Level.JUNIOR);
        assertThat(listed())
                .hasSize(9)
                .contains("q-sql-easy", "c-sql")
                .doesNotContain("q-sql-pleno", "q-java-pleno", "q-sql-senior", "c-sql-senior");

        world.withGoal(Level.PLENO);
        assertThat(listed())
                .hasSize(11)
                .contains("q-sql-pleno", "q-java-pleno")
                .doesNotContain("q-sql-senior", "c-sql-senior");
        assertThat(page(null, null).totalItems()).isEqualTo(11);
        assertThat(page(QuestionStatus.NOT_STARTED, null).totalItems()).isEqualTo(11);

        world.withGoal(Level.SENIOR);
        assertThat(listed())
                .hasSize(13)
                .contains("q-sql-pleno", "q-java-pleno", "q-sql-senior", "c-sql-senior");
    }

    @Test
    void eachItemOfTheListCarriesItsLevel() {
        world.withGoal(Level.SENIOR);

        assertThat(page(null, null).items())
                .filteredOn(item -> item.problem().slug().startsWith("q-sql-"))
                .extracting(item -> item.problem().slug(), item -> item.problem().level())
                .containsExactlyInAnyOrder(
                        tuple("q-sql-easy", Level.JUNIOR),
                        tuple("q-sql-hard", Level.JUNIOR),
                        tuple("q-sql-pleno", Level.PLENO),
                        tuple("q-sql-senior", Level.SENIOR));
    }

    @Test
    void theLevelFilterKeepsOneLevelAndALevelAboveTheGoalFindsNothing() {
        world.withGoal(Level.PLENO);

        assertThat(listed(Level.PLENO)).containsExactlyInAnyOrder("q-sql-pleno", "q-java-pleno");
        assertThat(listed(Level.JUNIOR)).hasSize(9).doesNotContain("q-sql-pleno", "q-java-pleno");
        assertThat(listed(Level.SENIOR)).isEmpty();
        assertThat(page(null, Level.SENIOR).totalPages()).isEqualTo(1);
    }

    @Test
    void aQuestionAboveTheGoalIsNotFoundToOpenNorToAnswer() {
        world.withGoal(Level.PLENO);
        UUID right = world.question("q-sql-senior").correctOption().id();

        assertThat(getQuestion.execute(world.user, "q-sql-pleno").question().level())
                .isEqualTo(Level.PLENO);
        assertThatThrownBy(() -> getQuestion.execute(world.user, "q-sql-senior"))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Questão não encontrada: q-sql-senior.");
        assertThatThrownBy(() -> answerQuestion.execute(world.user, "q-sql-senior", right))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Questão não encontrada: q-sql-senior.");
        assertThat(world.answers.findByUser(world.user)).isEmpty();
    }

    @Test
    void withoutAGoalOnlyJuniorOpens() {
        assertThat(getQuestion.execute(world.user, "q-java").question().slug()).isEqualTo("q-java");
        assertThatThrownBy(() -> getQuestion.execute(world.user, "q-sql-pleno"))
                .isInstanceOf(QuestionNotFoundException.class);
        assertThatThrownBy(() -> getChallenge.execute(world.user, "c-sql-senior"))
                .isInstanceOf(ChallengeNotFoundException.class);
    }

    @Test
    void aChallengeAboveTheGoalIsNotFoundToOpenRunNorSubmit() {
        world.withGoal(Level.PLENO);

        assertThatThrownBy(() -> getChallenge.execute(world.user, "c-sql-senior"))
                .isInstanceOf(ChallengeNotFoundException.class)
                .hasMessage("Desafio não encontrado: c-sql-senior.");
        assertThatThrownBy(() -> run.execute(world.user, "c-sql-senior", Language.PYTHON, CODE))
                .isInstanceOf(ChallengeNotFoundException.class);
        assertThatThrownBy(() -> submit.execute(world.user, "c-sql-senior", Language.PYTHON, CODE))
                .isInstanceOf(ChallengeNotFoundException.class);
        assertThat(queue.published()).isEmpty();

        world.withGoal(Level.SENIOR);
        assertThat(getChallenge.execute(world.user, "c-sql-senior").slug())
                .isEqualTo("c-sql-senior");
        run.execute(world.user, "c-sql-senior", Language.PYTHON, CODE);
        submit.execute(world.user, "c-sql-senior", Language.PYTHON, CODE);
        assertThat(queue.published()).hasSize(2);
    }

    @Test
    void theDailyQuestionIsDrawnAmongTheVisible() {
        for (String slug : List.of("q-sql-easy", "q-java-sql", "q-sql-hard", "q-java", "q-http")) {
            world.answered(world.user, slug, true, 20, T0.minusSeconds(2 * 86_400));
        }

        world.withGoal(Level.JUNIOR);
        assertThat(getDaily.execute(world.user).question().slug()).isEqualTo("q-sql-easy");

        world.withGoal(Level.PLENO);
        assertThat(getDaily.execute(world.user).question().slug()).isEqualTo("q-sql-pleno");
    }

    @Test
    void theCompetencyPanelListsOnlyTheVisibleWithTheirLevel() {
        world.withGoal(Level.JUNIOR);
        TrackCompetencyDetail junior = getCompetency.execute(world.user, "sql");
        assertThat(junior.questions())
                .extracting(TrackQuestionView::slug)
                .containsExactly("q-sql-easy", "q-java-sql", "q-sql-hard");
        assertThat(junior.challenges())
                .extracting(TrackChallengeView::slug)
                .containsExactly("c-sql");

        world.withGoal(Level.SENIOR);
        TrackCompetencyDetail senior = getCompetency.execute(world.user, "sql");
        assertThat(senior.questions())
                .extracting(TrackQuestionView::slug, TrackQuestionView::level)
                .containsExactly(
                        tuple("q-sql-easy", Level.JUNIOR),
                        tuple("q-java-sql", Level.JUNIOR),
                        tuple("q-sql-pleno", Level.PLENO),
                        tuple("q-sql-hard", Level.JUNIOR),
                        tuple("q-sql-senior", Level.SENIOR));
        assertThat(senior.challenges())
                .extracting(TrackChallengeView::slug, TrackChallengeView::level)
                .containsExactly(tuple("c-sql", Level.JUNIOR), tuple("c-sql-senior", Level.SENIOR));
    }

    @Test
    void theStatusOfTheCompetencyIgnoresWhatIsAboveTheGoal() {
        for (String slug : List.of("q-sql-easy", "q-java-sql", "q-sql-hard", "c-sql")) {
            world.progress.record(world.user, slug, QuestionProgress.SOLVED);
        }

        world.withGoal(Level.JUNIOR);
        assertThat(statusOf("sql")).isEqualTo(CompetencyStatus.DONE);

        world.withGoal(Level.PLENO);
        assertThat(statusOf("sql")).isEqualTo(CompetencyStatus.IN_PROGRESS);
    }

    @Test
    void loweringTheGoalHidesTheItemButKeepsTheAnswerAndTheXp() {
        world.withGoal(Level.PLENO);
        UUID right = world.question("q-sql-pleno").correctOption().id();
        assertThat(answerQuestion.execute(world.user, "q-sql-pleno", right).xpGained())
                .isEqualTo(30);

        world.withGoal(Level.JUNIOR);

        assertThat(listed()).doesNotContain("q-sql-pleno");
        assertThatThrownBy(() -> getQuestion.execute(world.user, "q-sql-pleno"))
                .isInstanceOf(QuestionNotFoundException.class);
        assertThat(world.answers.findByUser(world.user)).hasSize(1);
        assertThat(getProgress.execute(world.user).xp()).isEqualTo(30);
    }

    @Test
    void theAnswerGivenInsideALessonDoesNotGoThroughTheLevelRule() {
        world.withGoal(Level.JUNIOR);
        UUID right = world.question("q-sql-pleno").correctOption().id();

        assertThat(answerQuestion.record(world.user, "q-sql-pleno", right).correct()).isTrue();
    }

    @Test
    void theLevelIs404BeforeTheExamLock() {
        world.withGoal(Level.PLENO);
        UUID session = UUID.randomUUID();
        UUID option = world.question("q-sql-senior").correctOption().id();
        world.examLocks.lockQuestion(world.user, world.question("q-sql-senior").id(), session);
        world.examLocks.lock(world.user, world.challenges.get("c-sql-senior").id(), session);

        assertThatThrownBy(() -> getQuestion.execute(world.user, "q-sql-senior"))
                .isInstanceOf(QuestionNotFoundException.class);
        assertThatThrownBy(() -> answerQuestion.execute(world.user, "q-sql-senior", option))
                .isInstanceOf(QuestionNotFoundException.class);
        assertThatThrownBy(() -> getChallenge.execute(world.user, "c-sql-senior"))
                .isInstanceOf(ChallengeNotFoundException.class);
        assertThatThrownBy(() -> run.execute(world.user, "c-sql-senior", Language.PYTHON, CODE))
                .isInstanceOf(ChallengeNotFoundException.class);
        assertThatThrownBy(() -> submit.execute(world.user, "c-sql-senior", Language.PYTHON, CODE))
                .isInstanceOf(ChallengeNotFoundException.class);
    }
}
