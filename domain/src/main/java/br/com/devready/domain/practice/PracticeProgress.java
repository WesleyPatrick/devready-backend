package br.com.devready.domain.practice;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public record PracticeProgress(
        int xp, int streakDays, DailyGoal dailyGoal, List<DayActivity> activity) {

    public static final int ACTIVITY_DAYS = 35;

    public PracticeProgress {
        activity = List.copyOf(activity);
    }

    public static PracticeProgress of(
            Collection<Answer> answers,
            Collection<ChallengeAttempt> attempts,
            Collection<CompletedSimulation> simulations,
            Instant now) {
        LocalDate today = PracticeCalendar.dayOf(now);
        Map<LocalDate, Integer> perDay = ActivityDays.of(answers, attempts, simulations);
        int xp = 0;
        for (Answer answer : answers) {
            xp += answer.xpGained();
        }
        for (ChallengeAttempt attempt : attempts) {
            if (attempt.counts()) {
                xp += attempt.xpGained();
            }
        }
        for (CompletedSimulation simulation : simulations) {
            xp += simulation.xpAwarded();
        }
        List<DayActivity> activity = new ArrayList<>();
        for (int back = ACTIVITY_DAYS - 1; back >= 0; back--) {
            LocalDate day = today.minusDays(back);
            activity.add(new DayActivity(day, perDay.getOrDefault(day, 0)));
        }
        return new PracticeProgress(
                xp, streak(perDay, today), DailyGoal.of(perDay.getOrDefault(today, 0)), activity);
    }

    private static int streak(Map<LocalDate, Integer> perDay, LocalDate today) {
        LocalDate day = perDay.containsKey(today) ? today : today.minusDays(1);
        int streak = 0;
        while (perDay.containsKey(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
