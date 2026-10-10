package br.com.devready.application.session.usecase;

import static br.com.devready.application.session.fake.SessionWorld.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.career.exception.CompetencyNotInTrackException;
import br.com.devready.application.session.exception.LessonUnavailableException;
import br.com.devready.application.session.exception.SessionInProgressException;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionQuestion;
import br.com.devready.domain.session.SessionStatus;
import org.junit.jupiter.api.Test;

class StartLessonUseCaseTest {

    private final SessionWorld world = new SessionWorld();
    private final StartLessonUseCase start =
            new StartLessonUseCase(
                    world.practice.profiles,
                    world.practice.assembler,
                    world.practice.areas,
                    world.pool,
                    world.practice.answers,
                    world.practice.simulations,
                    world.opener,
                    world.random,
                    world.ids,
                    world.clock);

    @Test
    void aLessonOfACompetencyOfTheTrackPutsTheUnsolvedFirst() {
        world.practice.withGoal();
        world.practice.answered(world.user, "q-sql-easy", true, 20, T0.minusSeconds(60));

        Session lesson = start.execute(world.user, "sql");

        assertThat(lesson.format()).isEqualTo(SessionFormat.LESSON);
        assertThat(lesson.competencySlug()).isEqualTo("sql");
        assertThat(lesson.level()).isEqualTo(Level.JUNIOR);
        assertThat(lesson.deadlineAt()).isNull();
        assertThat(lesson.areaId()).isEqualTo(world.backend().id());
        assertThat(lesson.questions())
                .extracting(SessionQuestion::questionId)
                .containsExactlyInAnyOrder(
                        world.id("q-sql-easy"), world.id("q-sql-hard"), world.id("q-java-sql"))
                .endsWith(world.id("q-sql-easy"));
        assertThat(world.sessions.findOpen(world.user)).contains(lesson);
    }

    @Test
    void aQuestionRightInACorrectedSimulationAlsoGoesToTheEnd() {
        world.practice.withGoal();
        world.practice.simulations.solveQuestion(world.user, world.id("q-sql-hard"));

        assertThat(start.execute(world.user, "sql").questions())
                .extracting(SessionQuestion::questionId)
                .endsWith(world.id("q-sql-hard"));
    }

    @Test
    void needsAGoalAndACompetencyOfTheTrack() {
        assertThatThrownBy(() -> start.execute(world.user, "sql"))
                .isInstanceOf(CareerProfileNotFoundException.class);
        world.practice.withGoal();
        assertThatThrownBy(() -> start.execute(world.user, "seguranca"))
                .isInstanceOf(CompetencyNotInTrackException.class);
    }

    @Test
    void withFewerThanThreeQuestionsThereIsNoLesson() {
        world.practice.withGoal();

        assertThatThrownBy(() -> start.execute(world.user, "java"))
                .isInstanceOf(LessonUnavailableException.class)
                .hasMessage("Java não tem questões suficientes para uma lição.");
        assertThat(world.sessions.findByUser(world.user)).isEmpty();
    }

    @Test
    void anOpenSimulationBlocksTheLessonAndAnOpenLessonIsClosed() {
        world.practice.withGoal();
        Session first = start.execute(world.user, "sql");

        Session second = start.execute(world.user, "sql");

        assertThat(world.sessions.get(first.id()).status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(world.sessions.findOpen(world.user)).contains(second);
        world.finishSession.execute(world.user, second.id());
        Session simulation = world.openSimulation();
        assertThatThrownBy(() -> start.execute(world.user, "sql"))
                .isInstanceOfSatisfying(
                        SessionInProgressException.class,
                        e -> assertThat(e.sessionId()).isEqualTo(simulation.id()));
    }

    @Test
    void theLessonDrawsOnlyTheQuestionsUpToTheGoal() {
        world.practice.withLevels();
        world.practice.withGoal(Level.PLENO);

        assertThat(start.execute(world.user, "sql").questions())
                .extracting(SessionQuestion::questionId)
                .containsExactlyInAnyOrder(
                        world.id("q-sql-easy"),
                        world.id("q-sql-hard"),
                        world.id("q-java-sql"),
                        world.id("q-sql-pleno"));
    }

    @Test
    void theMinimumOfThreeCountsOnlyTheVisibleQuestions() {
        world.practice.withLevels();
        world.practice.withGoal(Level.JUNIOR);
        assertThatThrownBy(() -> start.execute(world.user, "java"))
                .isInstanceOf(LessonUnavailableException.class);

        world.practice.withGoal(Level.PLENO);
        Session lesson = start.execute(world.user, "java");

        assertThat(lesson.level()).isEqualTo(Level.PLENO);
        assertThat(lesson.questions())
                .extracting(SessionQuestion::questionId)
                .containsExactlyInAnyOrder(
                        world.id("q-java"), world.id("q-java-sql"), world.id("q-java-pleno"));
    }
}
