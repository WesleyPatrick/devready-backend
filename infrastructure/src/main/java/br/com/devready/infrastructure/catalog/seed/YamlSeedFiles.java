package br.com.devready.infrastructure.catalog.seed;

import java.util.List;
import java.util.Map;

final class YamlSeedFiles {

    private YamlSeedFiles() {}

    record AreaFile(
            String slug,
            String name,
            int questionCount,
            int challengeCount,
            int durationMinutes,
            double objectiveWeight) {}

    record TagFile(String slug, String name) {}

    record QuestionsFile(String area, String level, List<QuestionItem> questions) {}

    record QuestionItem(
            String slug,
            String title,
            String difficulty,
            String statement,
            List<OptionItem> options,
            String explanation,
            List<String> tags) {}

    record OptionItem(String text, Boolean correct) {}

    record ChallengeFile(
            String slug,
            String title,
            String area,
            String level,
            String difficulty,
            List<String> tags,
            Integer timeLimitMs,
            Integer memoryLimitKb,
            String comparison,
            Integer maxSubmissions,
            String statement,
            String approachSummary,
            Map<String, LanguageItem> languages,
            List<TestCaseItem> testCases) {}

    record LanguageItem(String functionName, String starterCode, String referenceSolution) {}

    record TestCaseItem(Object args, Object expected, Boolean visible) {}
}
