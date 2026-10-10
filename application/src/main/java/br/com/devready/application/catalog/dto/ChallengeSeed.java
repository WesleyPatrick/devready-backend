package br.com.devready.application.catalog.dto;

import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.Map;

public record ChallengeSeed(
        String slug,
        String title,
        String areaSlug,
        Level level,
        Difficulty difficulty,
        String statement,
        String approachSummary,
        int timeLimitMs,
        int memoryLimitKb,
        ComparisonMode comparison,
        int maxSubmissions,
        Map<Language, LanguageSeed> languages,
        List<TestCaseSeed> testCases,
        List<String> tags) {
    public record LanguageSeed(String functionName, String starterCode, String referenceSolution) {}

    public record TestCaseSeed(String argsJson, String expectedJson, boolean visible) {}
}
