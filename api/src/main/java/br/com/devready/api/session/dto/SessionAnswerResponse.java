package br.com.devready.api.session.dto;

import br.com.devready.api.practice.dto.OptionResponse;
import br.com.devready.application.session.dto.SessionAnswer;
import br.com.devready.domain.gamification.Achievement;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

public record SessionAnswerResponse(
        int position,
        String slug,
        String title,
        String difficulty,
        String statement,
        List<OptionResponse> options,
        UUID chosenOptionId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean correct,
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID correctOptionId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String explanation,
        int xpGained,
        String sessionStatus,
        List<String> newAchievements) {

    public static SessionAnswerResponse from(SessionAnswer answer, List<Achievement> fresh) {
        SessionQuestionResponse q = SessionQuestionResponse.from(answer.question());
        return new SessionAnswerResponse(
                q.position(),
                q.slug(),
                q.title(),
                q.difficulty(),
                q.statement(),
                q.options(),
                q.chosenOptionId(),
                q.correct(),
                q.correctOptionId(),
                q.explanation(),
                answer.xpGained(),
                answer.sessionStatus().name(),
                fresh.stream().map(Achievement::slug).toList());
    }
}
