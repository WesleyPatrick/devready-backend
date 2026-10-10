package br.com.devready.application.practice.usecase;

import static br.com.devready.application.evaluation.fake.SubmissionFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.evaluation.fake.FixedLanguageRuntimes;
import br.com.devready.application.evaluation.fake.RecordingSubmissionQueue;
import br.com.devready.application.practice.dto.ChallengeLanguageView;
import br.com.devready.application.practice.dto.ChallengeView;
import br.com.devready.application.practice.exception.ChallengeInExamException;
import br.com.devready.application.practice.exception.ChallengeNotFoundException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.practice.QuestionStatus;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetChallengeUseCaseTest {

    private final PracticeWorld world = new PracticeWorld();
    private final SubmissionDesk desk =
            new SubmissionDesk(
                    world.challenges,
                    world.submissions,
                    new RecordingSubmissionQueue(),
                    new SequentialIdGenerator(),
                    new FixedClock(T0));
    private final GetChallengeUseCase getChallenge =
            new GetChallengeUseCase(
                    desk,
                    world.board,
                    world.attempts,
                    world.simulations,
                    world.examLocks,
                    new FixedLanguageRuntimes(),
                    world.levels);

    private Area backend() {
        return world.areas.findBySlug("backend").orElseThrow();
    }

    @Test
    void bringsWhatTheScreenNeedsWithoutTheReferenceSolution() {
        world.withGoal();

        ChallengeView view = getChallenge.execute(world.user, "c-sql");

        assertThat(view.slug()).isEqualTo("c-sql");
        assertThat(view.title()).isEqualTo("Mesclar");
        assertThat(view.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(view.statement()).isEqualTo("Enunciado");
        assertThat(view.competency().slug()).isEqualTo("sql");
        assertThat(view.askedBy()).containsExactly("nubank", "google");
        assertThat(view.status()).isEqualTo(QuestionStatus.NOT_STARTED);
        assertThat(view.xp()).isEqualTo(40);
        assertThat(view.languages())
                .extracting(ChallengeLanguageView::language)
                .containsExactly(Language.JAVA, Language.PYTHON, Language.JAVASCRIPT);
        assertThat(view.languages())
                .extracting(ChallengeLanguageView::runtime)
                .containsExactly("Java 13", "Python 3.8", "Node 12");
        assertThat(view.languages().get(0).functionName()).isEqualTo("f");
        assertThat(view.languages().get(0).starterCode()).isEqualTo("class Solution {}");
        assertThat(view.visibleCases()).extracting(TestCase::position).containsExactly(0, 1);
        assertThat(view.hiddenCaseCount()).isEqualTo(2);
        assertThat(view.limits().timeLimitMs()).isEqualTo(5000);
        assertThat(view.approachSummary()).isNull();
        assertThat(ChallengeView.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("challenge", "referenceSolution");
    }

    @Test
    void withoutAGoalTheCompetencyComesFromTheBaseAndNobodyAsks() {
        ChallengeView view = getChallenge.execute(world.user, "c-sql");

        assertThat(view.competency().slug()).isEqualTo("sql");
        assertThat(view.askedBy()).isEmpty();
    }

    @Test
    void theStatusComesFromTheSubmissionsAndTheApproachShowsAfterSolving() {
        world.submitted(world.user, "c-sql", SubmissionStatus.ERROR, 0, T0);
        assertThat(getChallenge.execute(world.user, "c-sql").status())
                .isEqualTo(QuestionStatus.NOT_STARTED);

        world.submitted(world.user, "c-sql", SubmissionStatus.FAILED, 0, T0.plusSeconds(1));
        world.submitted(UUID.randomUUID(), "c-sql", SubmissionStatus.PASSED, 40, T0);
        ChallengeView attempted = getChallenge.execute(world.user, "c-sql");
        assertThat(attempted.status()).isEqualTo(QuestionStatus.ATTEMPTED);
        assertThat(attempted.approachSummary()).isNull();

        world.submitted(world.user, "c-sql", SubmissionStatus.PASSED, 40, T0.plusSeconds(2));
        ChallengeView solved = getChallenge.execute(world.user, "c-sql");
        assertThat(solved.status()).isEqualTo(QuestionStatus.SOLVED);
        assertThat(solved.approachSummary()).isEqualTo("Abordagem");
    }

    @Test
    void onlyTheLanguagesOfTheChallengeShowUpInTheUsualOrder() {
        world.challenges.save(
                CatalogFixtures.challenge(
                        "c-py",
                        "Python e JavaScript",
                        backend(),
                        Difficulty.MEDIUM,
                        true,
                        List.of(Language.JAVASCRIPT, Language.PYTHON),
                        "sql"));

        ChallengeView view = getChallenge.execute(world.user, "c-py");

        assertThat(view.languages())
                .extracting(ChallengeLanguageView::language)
                .containsExactly(Language.PYTHON, Language.JAVASCRIPT);
        assertThat(view.xp()).isEqualTo(60);
    }

    @Test
    void anUnknownOrInactiveChallengeIsNotFound() {
        world.challenges.save(
                CatalogFixtures.challenge(
                        "c-off",
                        "Inativo",
                        backend(),
                        Difficulty.EASY,
                        false,
                        List.of(Language.JAVA),
                        "sql"));

        assertThatThrownBy(() -> getChallenge.execute(world.user, "nao-existe"))
                .isInstanceOf(ChallengeNotFoundException.class)
                .hasMessage("Desafio não encontrado: nao-existe.");
        assertThatThrownBy(() -> getChallenge.execute(world.user, "c-off"))
                .isInstanceOf(ChallengeNotFoundException.class);
    }

    @Test
    void aChallengeOfAnOpenSimulationIsLockedWithTheIdOfTheSession() {
        UUID session = UUID.randomUUID();
        world.examLocks.lock(world.user, world.challenges.get("c-sql").id(), session);

        assertThatThrownBy(() -> getChallenge.execute(world.user, "c-sql"))
                .isInstanceOfSatisfying(
                        ChallengeInExamException.class,
                        e -> assertThat(e.sessionId()).isEqualTo(session))
                .hasMessage("Este desafio está na sua simulação em andamento.");
        assertThat(getChallenge.execute(UUID.randomUUID(), "c-sql").slug()).isEqualTo("c-sql");
    }

    @Test
    void aChallengeAcceptedInACorrectedSimulationIsSolvedAndShowsTheApproach() {
        world.submittedInSession(
                world.user, "c-sql", SubmissionStatus.FAILED, 1, UUID.randomUUID(), T0);
        assertThat(getChallenge.execute(world.user, "c-sql").status())
                .isEqualTo(QuestionStatus.NOT_STARTED);

        world.simulations.solveChallenge(world.user, world.challenges.get("c-sql").id());
        ChallengeView solved = getChallenge.execute(world.user, "c-sql");

        assertThat(solved.status()).isEqualTo(QuestionStatus.SOLVED);
        assertThat(solved.approachSummary()).isEqualTo("Abordagem");
    }
}
