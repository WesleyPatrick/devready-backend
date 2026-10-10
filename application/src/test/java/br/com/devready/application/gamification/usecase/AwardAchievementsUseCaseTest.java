package br.com.devready.application.gamification.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.gamification.fake.InMemoryAchievementRepository;
import br.com.devready.application.gamification.port.AchievementRepository;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import br.com.devready.domain.practice.PracticeCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AwardAchievementsUseCaseTest {

    private static final LocalDate DAY = LocalDate.ofEpochDay(20_000);

    private final PracticeWorld world = new PracticeWorld();
    private final InMemoryAchievementRepository achievements = new InMemoryAchievementRepository();
    private final FixedClock clock = new FixedClock(at(0, 12));
    private final AwardAchievementsUseCase award =
            new AwardAchievementsUseCase(
                    world.answers,
                    world.attempts,
                    world.simulations,
                    world.assembler,
                    achievements,
                    clock);

    private static Instant at(int days, int hour) {
        return ZonedDateTime.of(DAY.plusDays(days), LocalTime.of(hour, 0), PracticeCalendar.ZONE)
                .toInstant();
    }

    @Test
    void theFirstAnswerUnlocksOnceWithTheTime() {
        assertThat(award.execute(world.user)).isEmpty();
        world.answered(world.user, "q-java", false, 0, at(0, 9));

        assertThat(award.execute(world.user)).containsExactly(Achievement.FIRST_QUESTION);
        assertThat(award.execute(world.user)).isEmpty();
        UnlockedAchievement saved = achievements.all().get(0);
        assertThat(saved.unlockedAt()).isEqualTo(at(0, 12));
        assertThat(saved.sessionId()).isNull();
    }

    @Test
    void tenDistinctSolvedQuestionsCountTheHitsOfSimulations() {
        for (String slug :
                new String[] {
                    "q-sql-easy",
                    "q-sql-hard",
                    "q-java-sql",
                    "q-java",
                    "q-http",
                    "q-seg",
                    "q-cripto"
                }) {
            world.answered(world.user, slug, true, 0, at(-40, 9));
        }
        world.answered(world.user, "q-none", true, 0, at(-40, 9));
        world.answered(world.user, "q-none", true, 0, at(-40, 10));
        world.simulations.solveQuestion(world.user, UUID.randomUUID());
        assertThat(award.execute(world.user)).doesNotContain(Achievement.TEN_QUESTIONS);

        world.simulations.solveQuestion(world.user, UUID.randomUUID());

        assertThat(award.execute(world.user)).containsExactly(Achievement.TEN_QUESTIONS);
    }

    @Test
    void theStreakTheXpAndTheSimulationsComeFromTheProgress() {
        world.answered(world.user, "q-java", false, 0, at(-2, 9));
        world.answered(world.user, "q-java", false, 0, at(-1, 9));
        world.simulations.complete(world.user, at(0, 10), 1_000);

        assertThat(award.execute(world.user))
                .containsExactly(
                        Achievement.FIRST_QUESTION,
                        Achievement.STREAK_3,
                        Achievement.XP_1000,
                        Achievement.FIRST_SIMULATION);
    }

    @Test
    void anEmptySimulationUnlocksNothing() {
        world.simulations.complete(world.user, at(0, 10), 0);

        assertThat(award.execute(world.user)).isEmpty();
    }

    @Test
    void theGoalCountsFiveDaysWithThreeActivitiesInTheWholeHistory() {
        for (int day = -100; day <= -60; day += 10) {
            for (int i = 0; i < 3; i++) {
                world.answered(world.user, "q-java", false, 0, at(day, 9 + i));
            }
        }

        assertThat(award.execute(world.user)).contains(Achievement.GOAL_FIVE_TIMES);
    }

    @Test
    void aDoneCompetencyOfTheTrackUnlocksTheFirstCompetency() {
        world.progress.record(world.user, "q-java", QuestionProgress.SOLVED);
        world.progress.record(world.user, "q-java-sql", QuestionProgress.SOLVED);
        assertThat(award.execute(world.user)).isEmpty();

        world.withGoal();

        assertThat(award.execute(world.user)).containsExactly(Achievement.FIRST_COMPETENCY);
    }

    @Test
    void whatTheCorrectionUnlockedKeepsTheSession() {
        UUID session = UUID.randomUUID();
        world.simulations.complete(world.user, at(0, 10), 50);

        assertThat(award.execute(world.user, session))
                .containsExactly(Achievement.FIRST_SIMULATION);
        assertThat(new GetSessionAchievementsUseCase(achievements).execute(world.user, session))
                .containsExactly(Achievement.FIRST_SIMULATION);
        assertThat(
                        new GetSessionAchievementsUseCase(achievements)
                                .execute(world.user, UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    void whatAConcurrentCheckAlreadySavedIsNotReportedAsNew() {
        world.answered(world.user, "q-java", false, 0, at(0, 9));
        achievements.unlock(
                new UnlockedAchievement(world.user, Achievement.FIRST_QUESTION, at(0, 11), null));
        AchievementRepository stale =
                new AchievementRepository() {
                    @Override
                    public List<UnlockedAchievement> findByUser(UUID userId) {
                        return List.of();
                    }

                    @Override
                    public boolean unlock(UnlockedAchievement unlocked) {
                        return achievements.unlock(unlocked);
                    }

                    @Override
                    public List<Achievement> unlockedBySession(UUID userId, UUID sessionId) {
                        return achievements.unlockedBySession(userId, sessionId);
                    }
                };
        AwardAchievementsUseCase racing =
                new AwardAchievementsUseCase(
                        world.answers,
                        world.attempts,
                        world.simulations,
                        world.assembler,
                        stale,
                        clock);

        assertThat(racing.execute(world.user)).isEmpty();
        assertThat(achievements.all()).hasSize(1);
        assertThat(achievements.all().get(0).unlockedAt()).isEqualTo(at(0, 11));
    }
}
