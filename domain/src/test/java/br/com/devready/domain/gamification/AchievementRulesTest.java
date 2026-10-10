package br.com.devready.domain.gamification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AchievementRulesTest {

    private static AchievementSnapshot nothing() {
        return new AchievementSnapshot(0, 0, 0, 0, 0, false, 0);
    }

    @Test
    void nothingDoneEarnsNothing() {
        assertThat(AchievementRules.earned(nothing())).isEmpty();
    }

    @Test
    void eachCriterionAtItsThreshold() {
        assertThat(AchievementRules.earned(new AchievementSnapshot(1, 0, 0, 0, 0, false, 0)))
                .containsExactly(Achievement.FIRST_QUESTION);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 10, 0, 0, 0, false, 0)))
                .containsExactly(Achievement.TEN_QUESTIONS);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 9, 0, 0, 0, false, 0)))
                .isEmpty();
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 3, 0, 0, false, 0)))
                .containsExactly(Achievement.STREAK_3);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 2, 0, 0, false, 0)))
                .isEmpty();
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 6, 0, 0, false, 0)))
                .containsExactly(Achievement.STREAK_3);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 7, 0, 0, false, 0)))
                .containsExactly(Achievement.STREAK_3, Achievement.STREAK_7);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 30, 0, 0, false, 0)))
                .contains(Achievement.STREAK_30);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 29, 0, 0, false, 0)))
                .doesNotContain(Achievement.STREAK_30);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 0, 999, 0, false, 0)))
                .isEmpty();
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 0, 1_000, 0, false, 0)))
                .containsExactly(Achievement.XP_1000);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 0, 4_999, 0, false, 0)))
                .containsExactly(Achievement.XP_1000);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 0, 5_000, 0, false, 0)))
                .containsExactly(Achievement.XP_1000, Achievement.XP_5000);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 0, 0, 5, false, 0)))
                .containsExactly(Achievement.GOAL_FIVE_TIMES);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 0, 0, 4, false, 0)))
                .isEmpty();
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 0, 0, 0, true, 0)))
                .containsExactly(Achievement.FIRST_COMPETENCY);
        assertThat(AchievementRules.earned(new AchievementSnapshot(0, 0, 0, 0, 0, false, 1)))
                .containsExactly(Achievement.FIRST_SIMULATION);
    }

    @Test
    void newlyEarnedLeavesOutWhatWasAlreadyUnlockedInTheOrderOfTheCatalog() {
        AchievementSnapshot snapshot = new AchievementSnapshot(12, 10, 7, 1_200, 0, false, 1);

        assertThat(AchievementRules.newlyEarned(snapshot, Set.of(Achievement.STREAK_3)))
                .containsExactly(
                        Achievement.FIRST_QUESTION,
                        Achievement.TEN_QUESTIONS,
                        Achievement.STREAK_7,
                        Achievement.XP_1000,
                        Achievement.FIRST_SIMULATION);
    }

    @Test
    void aNegativeCountIsABug() {
        assertThatThrownBy(() -> new AchievementSnapshot(-1, 0, 0, 0, 0, false, 0))
                .isInstanceOf(GamificationException.class);
    }

    @Test
    void anUnlockNeedsPersonAchievementAndTime() {
        assertThatThrownBy(
                        () ->
                                new UnlockedAchievement(
                                        null, Achievement.STREAK_3, java.time.Instant.EPOCH, null))
                .isInstanceOf(GamificationException.class);
    }

    @Test
    void newlyEarnedIsEmptyWhenEverythingIsAlreadyUnlocked() {
        AchievementSnapshot snapshot = new AchievementSnapshot(50, 50, 30, 5_000, 9, true, 3);

        assertThat(AchievementRules.earned(snapshot))
                .containsExactlyInAnyOrder(Achievement.values());
        assertThat(AchievementRules.newlyEarned(snapshot, Set.of(Achievement.values()))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void eachCountRejectsNegatives(int position) {
        int[] counts = new int[6];
        counts[position] = -1;

        assertThatThrownBy(
                        () ->
                                new AchievementSnapshot(
                                        counts[0], counts[1], counts[2], counts[3], counts[4],
                                        false, counts[5]))
                .isInstanceOf(GamificationException.class);
    }

    @Test
    void anUnlockWithoutAchievementOrTimeIsRefused() {
        assertThatThrownBy(
                        () -> new UnlockedAchievement(UUID.randomUUID(), null, Instant.EPOCH, null))
                .isInstanceOf(GamificationException.class);
        assertThatThrownBy(
                        () ->
                                new UnlockedAchievement(
                                        UUID.randomUUID(), Achievement.STREAK_3, null, null))
                .isInstanceOf(GamificationException.class);
    }
}
