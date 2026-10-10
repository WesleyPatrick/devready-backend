package br.com.devready.application.practice.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.practice.dto.QuestionView;
import br.com.devready.application.practice.exception.QuestionInExamException;
import br.com.devready.application.practice.exception.QuestionNotFoundException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.domain.practice.QuestionStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetQuestionUseCaseTest {

    private static final Instant NOW = Instant.ofEpochSecond(1_700_000_000L);

    private final PracticeWorld world = new PracticeWorld();
    private final GetQuestionUseCase getQuestion =
            new GetQuestionUseCase(
                    world.questions,
                    world.board,
                    world.answers,
                    world.simulations,
                    world.examLocks,
                    world.levels);

    @Test
    void bringsTheQuestionWithItsCompetencyTimeAndXp() {
        world.withGoal();

        QuestionView view = getQuestion.execute(world.user, "q-java-sql");

        assertThat(view.question().slug()).isEqualTo("q-java-sql");
        assertThat(view.question().options()).hasSize(3);
        assertThat(view.competency().slug()).isEqualTo("sql");
        assertThat(view.status()).isEqualTo(QuestionStatus.NOT_STARTED);
        assertThat(view.estimatedMinutes()).isEqualTo(4);
        assertThat(view.xp()).isEqualTo(30);
    }

    @Test
    void withoutAGoalTheCompetencyComesFromTheBase() {
        assertThat(getQuestion.execute(world.user, "q-java-sql").competency().slug())
                .isEqualTo("sql");
        assertThat(getQuestion.execute(world.user, "q-java").competency().slug()).isEqualTo("java");
        assertThat(getQuestion.execute(world.user, "q-none").competency()).isNull();
    }

    @Test
    void theStatusFollowsTheAnswersOfThePerson() {
        world.answered(world.user, "q-java", false, 0, NOW);
        assertThat(getQuestion.execute(world.user, "q-java").status())
                .isEqualTo(QuestionStatus.ATTEMPTED);

        world.answered(world.user, "q-java", true, 20, NOW.plusSeconds(60));
        assertThat(getQuestion.execute(world.user, "q-java").status())
                .isEqualTo(QuestionStatus.SOLVED);
        assertThat(getQuestion.execute(UUID.randomUUID(), "q-java").status())
                .isEqualTo(QuestionStatus.NOT_STARTED);
    }

    @Test
    void unknownOrInactiveQuestionIsNotFound() {
        assertThatThrownBy(() -> getQuestion.execute(world.user, "nao-existe"))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Questão não encontrada: nao-existe.");
        assertThatThrownBy(() -> getQuestion.execute(world.user, "q-off"))
                .isInstanceOf(QuestionNotFoundException.class);
    }

    @Test
    void aQuestionRightInACorrectedSimulationIsSolved() {
        world.answered(world.user, "q-java", false, 0, NOW);
        world.simulations.solveQuestion(world.user, world.question("q-java").id());

        assertThat(getQuestion.execute(world.user, "q-java").status())
                .isEqualTo(QuestionStatus.SOLVED);
        assertThat(getQuestion.execute(UUID.randomUUID(), "q-java").status())
                .isEqualTo(QuestionStatus.NOT_STARTED);
    }

    @Test
    void aQuestionOfAnOpenSimulationDoesNotOpenUntilTheCorrection() {
        UUID session = UUID.randomUUID();
        world.examLocks.lockQuestion(world.user, world.question("q-java").id(), session);

        assertThatThrownBy(() -> getQuestion.execute(world.user, "q-java"))
                .isInstanceOf(QuestionInExamException.class)
                .hasMessage("Esta questão está na sua simulação em andamento.")
                .extracting(e -> ((QuestionInExamException) e).sessionId())
                .isEqualTo(session);
        assertThat(getQuestion.execute(world.user, "q-http").question().slug()).isEqualTo("q-http");
        world.examLocks.unlockAll();
        world.examLocks.lock(world.user, world.question("q-java").id(), session);
        assertThat(getQuestion.execute(world.user, "q-java").question().slug()).isEqualTo("q-java");
    }
}
