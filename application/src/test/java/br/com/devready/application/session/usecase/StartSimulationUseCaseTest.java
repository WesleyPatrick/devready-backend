package br.com.devready.application.session.usecase;

import static br.com.devready.application.session.fake.SessionWorld.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.session.exception.InsufficientPoolException;
import br.com.devready.application.session.exception.SessionInProgressException;
import br.com.devready.application.session.exception.UnknownAreaException;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionChallenge;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionQuestion;
import br.com.devready.domain.session.SessionStatus;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StartSimulationUseCaseTest {

    private final SessionWorld world = new SessionWorld();
    private final StartSimulationUseCase start =
            new StartSimulationUseCase(
                    world.practice.profiles,
                    world.practice.areas,
                    world.pool,
                    world.opener,
                    world.random,
                    world.ids,
                    world.clock);

    @Test
    void drawsTheCountsOfTheAreaFromTheActiveItemsOfTheLevel() {
        world.practice.withGoal();

        Session session = start.execute(world.user, "backend", Level.JUNIOR);

        List<UUID> activeBackend =
                world.pool.activeQuestions(world.backend().id(), Level.JUNIOR).stream()
                        .map(Question::id)
                        .toList();
        assertThat(session.format()).isEqualTo(SessionFormat.SIMULATION);
        assertThat(session.level()).isEqualTo(Level.JUNIOR);
        assertThat(session.questions()).hasSize(8);
        assertThat(session.questions())
                .extracting(SessionQuestion::questionId)
                .containsExactlyInAnyOrderElementsOf(activeBackend)
                .doesNotContain(world.id("q-off"), world.id("q-front"));
        assertThat(session.challenges())
                .extracting(SessionChallenge::challengeId)
                .containsExactlyInAnyOrder(
                        world.practice.challenges.get("c-sql").id(),
                        world.practice.challenges.get("c-two").id());
        assertThat(session.deadlineAt()).isEqualTo(T0.plus(Duration.ofMinutes(45)));
        assertThat(session.objectiveWeight()).isEqualTo(0.5);
        assertThat(world.sessions.findOpen(world.user)).contains(session);
    }

    @Test
    void theSameSeedDrawsTheSameOrder() {
        world.practice.withGoal();
        Session first = start.execute(world.user, "backend", Level.JUNIOR);
        world.finishSession.execute(world.user, first.id());

        Session second =
                new StartSimulationUseCase(
                                world.practice.profiles,
                                world.practice.areas,
                                world.pool,
                                world.opener,
                                new java.util.Random(7)::nextInt,
                                world.ids,
                                world.clock)
                        .execute(world.user, "backend", Level.JUNIOR);

        assertThat(second.questions())
                .extracting(SessionQuestion::questionId)
                .containsExactlyElementsOf(
                        first.questions().stream().map(SessionQuestion::questionId).toList());
        assertThat(second.challenges())
                .extracting(SessionChallenge::challengeId)
                .containsExactlyElementsOf(
                        first.challenges().stream().map(SessionChallenge::challengeId).toList());
    }

    @Test
    void needsAGoal() {
        assertThatThrownBy(() -> start.execute(world.user, "backend", Level.JUNIOR))
                .isInstanceOf(CareerProfileNotFoundException.class);
    }

    @Test
    void anUnknownAreaOrAPoolTooSmallIsRefused() {
        world.practice.withGoal();

        assertThatThrownBy(() -> start.execute(world.user, "mobile", Level.JUNIOR))
                .isInstanceOf(UnknownAreaException.class)
                .hasMessage("Área desconhecida: mobile.");
        assertThatThrownBy(() -> start.execute(world.user, "backend", Level.PLENO))
                .isInstanceOf(InsufficientPoolException.class)
                .hasMessage(
                        "Não há questões e desafios suficientes para uma simulação de backend pleno.");
        assertThatThrownBy(() -> start.execute(world.user, "frontend", Level.JUNIOR))
                .isInstanceOf(InsufficientPoolException.class);
        assertThat(world.sessions.findByUser(world.user)).isEmpty();
    }

    @Test
    void anOpenSimulationIsAConflictWithItsId() {
        world.practice.withGoal();
        Session open = start.execute(world.user, "backend", Level.JUNIOR);

        assertThatThrownBy(() -> start.execute(world.user, "backend", Level.JUNIOR))
                .isInstanceOfSatisfying(
                        SessionInProgressException.class,
                        e -> assertThat(e.sessionId()).isEqualTo(open.id()))
                .hasMessage("Você já tem uma simulação em andamento.");
    }

    @Test
    void anOpenLessonIsClosedAndCorrectedBeforeTheSimulationStarts() {
        world.practice.withGoal();
        Session lesson = world.openLesson();

        Session simulation = start.execute(world.user, "backend", Level.JUNIOR);

        assertThat(world.sessions.get(lesson.id()).status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(world.sessions.get(lesson.id()).objectiveScore()).isZero();
        assertThat(world.sessions.findOpen(world.user)).contains(simulation);
    }

    @Test
    void aSimulationStartedAtTheSameTimeBecomesTheConflict() {
        world.practice.withGoal();
        UUID[] raced = new UUID[1];
        world.sessions.beforeNextCreate(() -> raced[0] = world.openSimulation().id());

        assertThatThrownBy(() -> start.execute(world.user, "backend", Level.JUNIOR))
                .isInstanceOfSatisfying(
                        SessionInProgressException.class,
                        e -> assertThat(e.sessionId()).isEqualTo(raced[0]));
    }
}
