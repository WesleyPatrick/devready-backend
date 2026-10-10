package br.com.devready.api.practice.dto;

import br.com.devready.application.practice.dto.ChallengeLanguageView;

public record ChallengeLanguageResponse(
        String language, String runtime, String functionName, String starterCode) {
    public static ChallengeLanguageResponse from(ChallengeLanguageView view) {
        return new ChallengeLanguageResponse(
                view.language().name(), view.runtime(), view.functionName(), view.starterCode());
    }
}
