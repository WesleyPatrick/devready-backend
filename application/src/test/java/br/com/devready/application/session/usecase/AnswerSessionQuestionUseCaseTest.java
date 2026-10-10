package br.com.devready.application.session.usecase;

import static br.com.devready.application.session.fake.SessionWorld.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.practice.exception.OptionNotInQuestionException;
import br.com.devready.application.session.dto.SessionAnswer;
import br.com.devready.application.session.exception.ItemNotInSessionException;
import br.com.devready.application.session.exception.QuestionAlreadyAnsweredException;
import br.com.devready.application.session.exception.SessionClosedException;
import br.com.devready.application.session.exception.SessionNotFoundException;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnswerSessionQuestionUseCaseTest {

    private final SessionWorld world = new SessionWorld();
    private final AnswerSessionQuestionUseCase answer =
            new AnswerSessionQuestionUseCase(
                    world.sessions,
                    world.pool,
                    world.answerQuestion,
                    world.finishSession,
                    world.transactions,
                    world.clock);

    @Test
    void inTheSimulationTheAnswerIsSavedWithoutRevealingAndCanChange() {
        Session session = world.openSimulation();
        int position = world.position(session, "q-java");

        SessionAnswer first =
                answer.execute(world.user, session.id(), position, world.option("q-java", false));
        SessionAnswer second =
                answer.execute(world.user, session.id(), position, world.option("q-java", true));

        assertThat(first.question().revealed()).isFalse();
        assertThat(first.question().correct()).isNull();
        assertThat(first.xpGained()).isZero();
        assertThat(second.question().chosenOptionId()).isEqualTo(world.option("q-java", true));
        assertThat(world.sessions.get(session.id()).question(position).orElseThrow().hit())
                .isTrue();
        assertThat(world.practice.answers.all()).isEmpty();
        assertThat(world.transactions.calls()).isZero();
    }

    @Test
    void inTheLessonTheAnswerRevealsAndCountsAsPractice() {
        Session lesson = world.openLesson();

        SessionAnswer result =
                answer.execute(world.user, lesson.id(), 1, world.option("q-sql-easy", true));

        assertThat(result.question().revealed()).isTrue();
        assertThat(result.question().correct()).isTrue();
        assertThat(result.question().question().slug()).isEqualTo("q-sql-easy");
        assertThat(result.xpGained()).isEqualTo(20);
        assertThat(result.sessionStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(world.practice.answers.all()).hasSize(1);
        assertThat(world.transactions.calls()).isEqualTo(1);
    }

    @Test
    void inTheLessonTheAnswerIsFinal() {
        Session lesson = world.openLesson();
        answer.execute(world.user, lesson.id(), 1, world.option("q-sql-easy", false));

        assertThatThrownBy(
                        () ->
                                answer.execute(
                                        world.user,
                                        lesson.id(),
                                        1,
                                        world.option("q-sql-easy", true)))
                .isInstanceOf(QuestionAlreadyAnsweredException.class);
        assertThat(world.practice.answers.all()).hasSize(1);
    }

    @Test
    void theLastAnswerOfTheLessonClosesAndCorrectsIt() {
        Session lesson = world.openLesson();
        answer.execute(world.user, lesson.id(), 1, world.option("q-sql-easy", true));
        answer.execute(world.user, lesson.id(), 2, world.option("q-sql-hard", false));

        SessionAnswer last =
                answer.execute(world.user, lesson.id(), 3, world.option("q-java-sql", true));

        assertThat(last.sessionStatus()).isEqualTo(SessionStatus.COMPLETED);
        Session corrected = world.sessions.get(lesson.id());
        assertThat(corrected.objectiveScore()).isEqualTo(0.6667);
        assertThat(corrected.xpAwarded()).isZero();
    }

    @Test
    void theLessonClosesWhenTheLastTwoAnswersArriveTogether() {
        Session lesson = world.openLesson();
        answer.execute(world.user, lesson.id(), 1, world.option("q-sql-easy", true));
        world.sessions.afterNextAnswer(
                () ->
                        world.sessions.answer(
                                lesson.id(), 3, world.option("q-java-sql", true), true, false));

        SessionAnswer second =
                answer.execute(world.user, lesson.id(), 2, world.option("q-sql-hard", true));

        assertThat(second.sessionStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(world.sessions.get(lesson.id()).status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(world.sessions.get(lesson.id()).objectiveScore()).isEqualTo(1.0);
    }

    @Test
    void answersAreAcceptedUntilThirtySecondsAfterTheDeadline() {
        Session session = world.openSimulation();
        int position = world.position(session, "q-java");
        world.clock.advanceTo(session.deadlineAt().plusSeconds(30));
        answer.execute(world.user, session.id(), position, world.option("q-java", true));

        world.clock.advanceTo(session.deadlineAt().plus(Duration.ofSeconds(31)));
        assertThatThrownBy(
                        () ->
                                answer.execute(
                                        world.user,
                                        session.id(),
                                        position,
                                        world.option("q-java", false)))
                .isInstanceOf(SessionClosedException.class);
        world.finishSession.execute(world.user, session.id());
        world.clock.advanceTo(T0);
        assertThatThrownBy(
                        () ->
                                answer.execute(
                                        world.user,
                                        session.id(),
                                        position,
                                        world.option("q-java", false)))
                .isInstanceOf(SessionClosedException.class);
    }

    @Test
    void refusesWhatIsNotInTheSession() {
        Session session = world.openSimulation();

        assertThatThrownBy(
                        () ->
                                answer.execute(
                                        world.user,
                                        session.id(),
                                        world.position(session, "q-java"),
                                        world.option("q-http", true)))
                .isInstanceOf(OptionNotInQuestionException.class);
        assertThatThrownBy(
                        () ->
                                answer.execute(
                                        world.user, session.id(), 9, world.option("q-java", true)))
                .isInstanceOf(ItemNotInSessionException.class)
                .hasMessage("Não existe questão 9 nesta sessão.");
        assertThatThrownBy(
                        () ->
                                answer.execute(
                                        UUID.randomUUID(),
                                        session.id(),
                                        1,
                                        world.option("q-java", true)))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void theLessonAnswersAQuestionThatALockedSimulationAlsoHas() {
        Session lesson = world.openLesson();
        world.practice.examLocks.lockQuestion(
                world.user, world.id("q-sql-easy"), UUID.randomUUID());

        SessionAnswer result =
                answer.execute(world.user, lesson.id(), 1, world.option("q-sql-easy", true));

        assertThat(result.question().correct()).isTrue();
        assertThat(result.xpGained()).isEqualTo(20);
        assertThat(world.practice.answers.all()).hasSize(1);
    }
}
