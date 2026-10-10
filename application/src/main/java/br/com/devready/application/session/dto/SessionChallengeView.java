package br.com.devready.application.session.dto;

import br.com.devready.application.practice.dto.ChallengeLanguageView;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.ExecutionLimits;
import br.com.devready.domain.catalog.TestCase;
import java.util.List;
import java.util.UUID;

public record SessionChallengeView(
        int position,
        String slug,
        String title,
        Difficulty difficulty,
        String statement,
        List<ChallengeLanguageView> languages,
        List<TestCase> visibleCases,
        ExecutionLimits limits,
        int submissionsUsed,
        int submissionsLeft,
        UUID bestSubmissionId,
        UUID lastSubmissionId) {
    public SessionChallengeView {
        languages = List.copyOf(languages);
        visibleCases = List.copyOf(visibleCases);
    }
}
