package br.com.devready.application.practice.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.practice.dto.QuestionView;
import br.com.devready.application.practice.exception.NoDailyQuestionException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.practice.PracticeCalendar;
import br.com.devready.domain.practice.QuestionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetDailyQuestionUseCaseTest {

    private static final LocalDate DAY = LocalDate.ofEpochDay(20_000);

    private final PracticeWorld world = new PracticeWorld();
    private final FixedClock clock = new FixedClock(at(0, 10, 0));
    private final GetDailyQuestionUseCase getDaily =
            new GetDailyQuestionUseCase(
                    world.board, world.questions, world.answers, world.simulations, clock);

    private static Instant at(int days, int hour, int minute) {
        return ZonedDateTime.of(
                        DAY.plusDays(days), LocalTime.of(hour, minute), PracticeCalendar.ZONE)
                .toInstant();
    }

    @Test
    void needsAGoal() {
        assertThatThrownBy(() -> getDaily.execute(world.user))
                .isInstanceOf(CareerProfileNotFoundException.class);
    }

    @Test
    void isTheFirstQuestionOfTheTrackWithItsCompetency() {
        world.withGoal();

        QuestionView daily = getDaily.execute(world.user);

        assertThat(daily.question().slug()).isEqualTo("q-sql-easy");
        assertThat(daily.competency().slug()).isEqualTo("sql");
        assertThat(daily.status()).isEqualTo(QuestionStatus.NOT_STARTED);
        assertThat(daily.estimatedMinutes()).isEqualTo(2);
        assertThat(daily.xp()).isEqualTo(20);
    }

    @Test
    void staysTheSameAfterBeingSolvedUntilMidnightInBrasilia() {
        world.withGoal();
        world.answered(world.user, "q-sql-easy", true, 20, at(0, 10, 5));
        clock.advanceTo(at(0, 23, 59));

        QuestionView sameDay = getDaily.execute(world.user);

        assertThat(sameDay.question().slug()).isEqualTo("q-sql-easy");
        assertThat(sameDay.status()).isEqualTo(QuestionStatus.SOLVED);
        clock.advanceTo(at(1, 0, 0));
        assertThat(getDaily.execute(world.user).question().slug()).isEqualTo("q-java-sql");
    }

    @Test
    void aQuestionSolvedInASimulationIsSolvedHereToo() {
        world.withGoal();
        world.simulations.solveQuestion(world.user, world.question("q-sql-easy").id());

        assertThat(getDaily.execute(world.user).question().slug()).isEqualTo("q-java-sql");
        world.simulations.solveQuestion(world.user, world.question("q-java-sql").id());
        world.simulations.solveQuestion(world.user, world.question("q-sql-hard").id());
        world.simulations.solveQuestion(world.user, world.question("q-java").id());
        world.simulations.solveQuestion(world.user, world.question("q-http").id());
        QuestionView back = getDaily.execute(world.user);
        assertThat(back.question().slug()).isEqualTo("q-sql-easy");
        assertThat(back.status()).isEqualTo(QuestionStatus.SOLVED);
    }

    @Test
    void aWrongAnswerYesterdayKeepsTheQuestion() {
        world.withGoal();
        world.answered(world.user, "q-sql-easy", false, 0, at(-1, 20, 0));

        QuestionView daily = getDaily.execute(world.user);

        assertThat(daily.question().slug()).isEqualTo("q-sql-easy");
        assertThat(daily.status()).isEqualTo(QuestionStatus.ATTEMPTED);
    }

    @Test
    void withEveryTrackQuestionSolvedItIsTheFirstAgain() {
        world.withGoal();
        for (String slug : List.of("q-sql-easy", "q-java-sql", "q-sql-hard", "q-java", "q-http")) {
            world.answered(world.user, slug, true, 0, at(-1, 9, 0));
        }

        assertThat(getDaily.execute(world.user).question().slug()).isEqualTo("q-sql-easy");
    }

    @Test
    void aTrackWithoutQuestionsHasNoDailyQuestion() {
        world.profiles.save(
                new CareerProfile(world.user, List.of("nubank"), "react", Level.JUNIOR));

        assertThatThrownBy(() -> getDaily.execute(world.user))
                .isInstanceOf(NoDailyQuestionException.class)
                .hasMessage("Sua trilha ainda não tem questão objetiva.");
    }
}
