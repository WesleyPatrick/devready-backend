package br.com.devready.api.practice.dto;

import br.com.devready.application.practice.dto.AnswerResult;
import br.com.devready.domain.gamification.Achievement;
import java.util.List;
import java.util.UUID;

public record AnswerResultResponse(
        boolean correct,
        UUID correctOptionId,
        String explanation,
        int xpGained,
        List<String> newAchievements) {
    public static AnswerResultResponse from(AnswerResult result, List<Achievement> fresh) {
        return new AnswerResultResponse(
                result.correct(),
                result.correctOptionId(),
                result.explanation(),
                result.xpGained(),
                fresh.stream().map(Achievement::slug).toList());
    }
}
