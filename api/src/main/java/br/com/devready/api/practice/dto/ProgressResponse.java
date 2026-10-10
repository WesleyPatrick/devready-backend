package br.com.devready.api.practice.dto;

import br.com.devready.domain.practice.PracticeProgress;
import java.util.List;

public record ProgressResponse(
        int xp, int streakDays, DailyGoalResponse dailyGoal, List<DayActivityResponse> activity) {
    public static ProgressResponse from(PracticeProgress progress) {
        return new ProgressResponse(
                progress.xp(),
                progress.streakDays(),
                new DailyGoalResponse(progress.dailyGoal().done(), progress.dailyGoal().target()),
                progress.activity().stream()
                        .map(day -> new DayActivityResponse(day.date().toString(), day.count()))
                        .toList());
    }
}
