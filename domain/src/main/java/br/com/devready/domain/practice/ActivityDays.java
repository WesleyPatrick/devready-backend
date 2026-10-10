package br.com.devready.domain.practice;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ActivityDays {

    private ActivityDays() {}

    public static Map<LocalDate, Integer> of(
            Collection<Answer> answers,
            Collection<ChallengeAttempt> attempts,
            Collection<CompletedSimulation> simulations) {
        Map<LocalDate, Integer> perDay = new HashMap<>();
        for (Answer answer : answers) {
            perDay.merge(PracticeCalendar.dayOf(answer.answeredAt()), 1, Integer::sum);
        }
        for (ChallengeAttempt attempt : attempts) {
            if (attempt.counts()) {
                perDay.merge(PracticeCalendar.dayOf(attempt.submittedAt()), 1, Integer::sum);
            }
        }
        for (CompletedSimulation simulation : simulations) {
            if (simulation.counts()) {
                perDay.merge(PracticeCalendar.dayOf(simulation.finishedAt()), 1, Integer::sum);
            }
        }
        return perDay;
    }

    public static int goalDays(Map<LocalDate, Integer> perDay) {
        return (int) perDay.values().stream().filter(count -> count >= DailyGoal.TARGET).count();
    }
}
