package br.com.devready.api.session.dto;

import br.com.devready.application.session.dto.SessionResultView;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.session.Session;
import java.time.Instant;
import java.util.List;

public record SessionResultResponse(
        String status,
        Double finalScore,
        double objectiveScore,
        Double challengeScore,
        int xpAwarded,
        Instant finishedAt,
        List<String> newAchievements,
        List<CompetencyResultResponse> competencies,
        List<QuestionCorrectionResponse> questions,
        List<ChallengeCorrectionResponse> challenges) {

    public static SessionResultResponse from(SessionResultView view, List<Achievement> fresh) {
        Session s = view.session();
        return new SessionResultResponse(
                s.status().name(),
                s.finalScore(),
                s.objectiveScore(),
                s.challengeScore(),
                s.xpAwarded(),
                s.finishedAt(),
                fresh.stream().map(Achievement::slug).toList(),
                view.competencies().stream().map(CompetencyResultResponse::from).toList(),
                view.questions().stream().map(QuestionCorrectionResponse::from).toList(),
                view.challenges().stream().map(ChallengeCorrectionResponse::from).toList());
    }
}
