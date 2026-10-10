package br.com.devready.api.session.dto;

import br.com.devready.api.practice.dto.ChallengeLanguageResponse;
import br.com.devready.api.practice.dto.LimitsResponse;
import br.com.devready.api.practice.dto.VisibleCaseResponse;
import br.com.devready.application.session.dto.SessionChallengeView;
import java.util.List;
import java.util.UUID;

public record SessionChallengeResponse(
        int position,
        String slug,
        String title,
        String difficulty,
        String statement,
        List<ChallengeLanguageResponse> languages,
        List<VisibleCaseResponse> visibleCases,
        LimitsResponse limits,
        int submissionsUsed,
        int submissionsLeft,
        UUID bestSubmissionId,
        UUID lastSubmissionId) {

    public static SessionChallengeResponse from(SessionChallengeView view) {
        return new SessionChallengeResponse(
                view.position(),
                view.slug(),
                view.title(),
                view.difficulty().name(),
                view.statement(),
                view.languages().stream().map(ChallengeLanguageResponse::from).toList(),
                view.visibleCases().stream().map(VisibleCaseResponse::from).toList(),
                LimitsResponse.from(view.limits()),
                view.submissionsUsed(),
                view.submissionsLeft(),
                view.bestSubmissionId(),
                view.lastSubmissionId());
    }
}
