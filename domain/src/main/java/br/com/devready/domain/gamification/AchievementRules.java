package br.com.devready.domain.gamification;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AchievementRules {

    private AchievementRules() {}

    public static Set<Achievement> earned(AchievementSnapshot s) {
        EnumSet<Achievement> earned = EnumSet.noneOf(Achievement.class);
        if (s.answers() >= 1) {
            earned.add(Achievement.FIRST_QUESTION);
        }
        if (s.solvedQuestions() >= 10) {
            earned.add(Achievement.TEN_QUESTIONS);
        }
        if (s.streakDays() >= 3) {
            earned.add(Achievement.STREAK_3);
        }
        if (s.streakDays() >= 7) {
            earned.add(Achievement.STREAK_7);
        }
        if (s.streakDays() >= 30) {
            earned.add(Achievement.STREAK_30);
        }
        if (s.xp() >= 1_000) {
            earned.add(Achievement.XP_1000);
        }
        if (s.xp() >= 5_000) {
            earned.add(Achievement.XP_5000);
        }
        if (s.goalDays() >= 5) {
            earned.add(Achievement.GOAL_FIVE_TIMES);
        }
        if (s.competencyDone()) {
            earned.add(Achievement.FIRST_COMPETENCY);
        }
        if (s.completedSimulations() >= 1) {
            earned.add(Achievement.FIRST_SIMULATION);
        }
        return earned;
    }

    public static List<Achievement> newlyEarned(
            AchievementSnapshot snapshot, Set<Achievement> unlocked) {
        return earned(snapshot).stream().filter(a -> !unlocked.contains(a)).toList();
    }
}
