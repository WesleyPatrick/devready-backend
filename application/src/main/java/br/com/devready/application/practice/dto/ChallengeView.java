package br.com.devready.application.practice.dto;

import br.com.devready.domain.career.Competency;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.ExecutionLimits;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.practice.QuestionStatus;
import java.util.List;

public record ChallengeView(
        String slug,
        String title,
        Difficulty difficulty,
        String statement,
        ExecutionLimits limits,
        Competency competency,
        List<String> askedBy,
        QuestionStatus status,
        int xp,
        List<ChallengeLanguageView> languages,
        List<TestCase> visibleCases,
        int hiddenCaseCount,
        String approachSummary) {
    public ChallengeView {
        askedBy = List.copyOf(askedBy);
        languages = List.copyOf(languages);
        visibleCases = List.copyOf(visibleCases);
    }
}
