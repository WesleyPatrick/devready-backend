package br.com.devready.api.practice.dto;

import br.com.devready.application.practice.dto.ChallengeView;
import java.util.List;

public record ChallengeResponse(
        String slug,
        String title,
        String difficulty,
        CompetencyRefResponse competency,
        List<String> askedBy,
        String status,
        int xp,
        String statement,
        List<ChallengeLanguageResponse> languages,
        List<VisibleCaseResponse> visibleCases,
        int hiddenCaseCount,
        LimitsResponse limits,
        String approachSummary) {

    public static ChallengeResponse from(ChallengeView view) {
        return new ChallengeResponse(
                view.slug(),
                view.title(),
                view.difficulty().name(),
                CompetencyRefResponse.from(view.competency()),
                view.askedBy(),
                view.status().name(),
                view.xp(),
                view.statement(),
                view.languages().stream().map(ChallengeLanguageResponse::from).toList(),
                view.visibleCases().stream().map(VisibleCaseResponse::from).toList(),
                view.hiddenCaseCount(),
                LimitsResponse.from(view.limits()),
                view.approachSummary());
    }
}
