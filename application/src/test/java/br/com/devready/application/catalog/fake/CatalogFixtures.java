package br.com.devready.application.catalog.fake;

import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.ChallengeLanguage;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.ExecutionLimits;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.catalog.TestCase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CatalogFixtures {

    private CatalogFixtures() {}

    public static Area area(String slug) {
        return new Area(UUID.randomUUID(), slug, slug, new SessionConfig(8, 2, 45, 0.5), true);
    }

    public static Question question(
            String slug,
            String title,
            Area area,
            Difficulty difficulty,
            boolean active,
            String... tags) {
        return new Question(
                UUID.randomUUID(),
                slug,
                title,
                area.id(),
                Level.JUNIOR,
                difficulty,
                "Enunciado de " + slug,
                "Explicação de " + slug,
                List.of(
                        new QuestionOption(UUID.randomUUID(), "A", false, 0),
                        new QuestionOption(UUID.randomUUID(), "B", true, 1),
                        new QuestionOption(UUID.randomUUID(), "C", false, 2)),
                Set.of(tags),
                active);
    }

    public static Challenge challenge(
            String slug, String title, Area area, Difficulty difficulty, String... tags) {
        return challenge(
                slug,
                title,
                area,
                difficulty,
                true,
                List.of(Language.JAVA, Language.PYTHON, Language.JAVASCRIPT),
                tags);
    }

    public static Challenge challenge(
            String slug,
            String title,
            Area area,
            Difficulty difficulty,
            boolean active,
            List<Language> languages,
            String... tags) {
        Map<Language, ChallengeLanguage> entries = new LinkedHashMap<>();
        for (Language language : languages) {
            entries.put(language, new ChallengeLanguage(language, "f", "class Solution {}", "ref"));
        }
        return new Challenge(
                UUID.randomUUID(),
                slug,
                title,
                area.id(),
                Level.JUNIOR,
                difficulty,
                "Enunciado",
                "Abordagem",
                new ExecutionLimits(5000, 262144),
                ComparisonMode.EXACT,
                5,
                entries,
                List.of(
                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                Set.of(tags),
                active);
    }

    public static Question atLevel(Question q, Level level) {
        return new Question(
                q.id(),
                q.slug(),
                q.title(),
                q.areaId(),
                level,
                q.difficulty(),
                q.statement(),
                q.explanation(),
                q.options(),
                q.tagSlugs(),
                q.active());
    }

    public static Challenge atLevel(Challenge c, Level level) {
        return new Challenge(
                c.id(),
                c.slug(),
                c.title(),
                c.areaId(),
                level,
                c.difficulty(),
                c.statement(),
                c.approachSummary(),
                c.limits(),
                c.comparisonMode(),
                c.maxSubmissions(),
                c.languages(),
                c.testCases(),
                c.tagSlugs(),
                c.active());
    }
}
