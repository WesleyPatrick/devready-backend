package br.com.devready.application.practice.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.practice.dto.AnswerResult;
import br.com.devready.application.practice.exception.OptionNotInQuestionException;
import br.com.devready.application.practice.exception.QuestionInExamException;
import br.com.devready.application.practice.exception.QuestionNotFoundException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.practice.Answer;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnswerQuestionUseCaseTest {

    private static final Instant NOW = Instant.ofEpochSecond(1_700_000_000L);

    private final PracticeWorld world = new PracticeWorld();
    private final FixedClock clock = new FixedClock(NOW);
    private final AnswerQuestionUseCase answerQuestion =
            new AnswerQuestionUseCase(
                    world.questions,
                    world.answers,
                    world.examLocks,
                    world.levels,
                    new SequentialIdGenerator(),
                    clock);

    private UUID right(String slug) {
        return world.question(slug).correctOption().id();
    }

    private UUID wrong(String slug) {
        return world.question(slug).options().get(0).id();
    }

    @Test
    void aWrongAnswerIsKeptWithoutXpAndShowsTheRightOption() {
        AnswerResult result = answerQuestion.execute(world.user, "q-java-sql", wrong("q-java-sql"));

        assertThat(result.correct()).isFalse();
        assertThat(result.xpGained()).isZero();
        assertThat(result.correctOptionId()).isEqualTo(right("q-java-sql"));
        assertThat(result.explanation()).isEqualTo("Explicação de q-java-sql");
        Answer saved = world.answers.all().get(0);
        assertThat(saved.id()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(saved.userId()).isEqualTo(world.user);
        assertThat(saved.questionId()).isEqualTo(world.question("q-java-sql").id());
        assertThat(saved.optionId()).isEqualTo(wrong("q-java-sql"));
        assertThat(saved.correct()).isFalse();
        assertThat(saved.answeredAt()).isEqualTo(NOW);
    }

    @Test
    void onlyTheFirstSolveEarnsXpAndItDependsOnTheDifficulty() {
        answerQuestion.execute(world.user, "q-java-sql", wrong("q-java-sql"));
        clock.advanceTo(NOW.plusSeconds(60));

        AnswerResult first = answerQuestion.execute(world.user, "q-java-sql", right("q-java-sql"));

        assertThat(first.correct()).isTrue();
        assertThat(first.xpGained()).isEqualTo(30);
        assertThat(answerQuestion.execute(world.user, "q-java-sql", right("q-java-sql")).xpGained())
                .isZero();
        assertThat(answerQuestion.execute(world.user, "q-java-sql", wrong("q-java-sql")).xpGained())
                .isZero();
        assertThat(world.answers.all()).extracting(Answer::xpGained).containsExactly(0, 30, 0, 0);
    }

    @Test
    void xpIsCountedPerPerson() {
        answerQuestion.execute(world.user, "q-sql-hard", right("q-sql-hard"));

        assertThat(
                        answerQuestion
                                .execute(UUID.randomUUID(), "q-sql-hard", right("q-sql-hard"))
                                .xpGained())
                .isEqualTo(40);
    }

    @Test
    void anOptionOfAnotherQuestionIsRefusedAndNothingIsKept() {
        assertThatThrownBy(() -> answerQuestion.execute(world.user, "q-java", right("q-http")))
                .isInstanceOf(OptionNotInQuestionException.class)
                .hasMessage("A alternativa escolhida não é desta questão.");
        assertThat(world.answers.all()).isEmpty();
    }

    @Test
    void unknownOrInactiveQuestionIsNotFound() {
        assertThatThrownBy(
                        () -> answerQuestion.execute(world.user, "nao-existe", UUID.randomUUID()))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Questão não encontrada: nao-existe.");
        assertThatThrownBy(() -> answerQuestion.execute(world.user, "q-off", right("q-off")))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    void answeringDoesNotNeedAGoal() {
        assertThat(answerQuestion.execute(world.user, "q-http", right("q-http")).xpGained())
                .isEqualTo(20);
    }

    @Test
    void aQuestionOfAnOpenSimulationIsNotAnsweredHereAndTheOthersAre() {
        UUID session = UUID.randomUUID();
        world.examLocks.lockQuestion(world.user, world.question("q-java").id(), session);

        assertThatThrownBy(() -> answerQuestion.execute(world.user, "q-java", right("q-java")))
                .isInstanceOf(QuestionInExamException.class)
                .hasMessage("Esta questão está na sua simulação em andamento.")
                .extracting(e -> ((QuestionInExamException) e).sessionId())
                .isEqualTo(session);
        assertThat(world.answers.all()).isEmpty();
        assertThat(answerQuestion.execute(world.user, "q-http", right("q-http")).correct())
                .isTrue();
        assertThat(answerQuestion.execute(UUID.randomUUID(), "q-java", right("q-java")).correct())
                .isTrue();

        world.examLocks.unlockAll();
        assertThat(answerQuestion.execute(world.user, "q-java", right("q-java")).correct())
                .isTrue();
    }
}
