package br.com.devready.infrastructure.session.persistence;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class SessionTestData {

    private SessionTestData() {}

    static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    static Area area(String slug) {
        return new Area(
                UUID.randomUUID(), slug, "Área " + slug, new SessionConfig(2, 1, 45, 0.5), true);
    }

    static Question question(String slug, Area area, Level level, boolean active, String tag) {
        return new Question(
                UUID.randomUUID(),
                slug,
                "Título " + slug,
                area.id(),
                level,
                Difficulty.EASY,
                "Enunciado",
                "Explicação",
                List.of(
                        new QuestionOption(UUID.randomUUID(), "A", false, 0),
                        new QuestionOption(UUID.randomUUID(), "B", true, 1)),
                Set.of(tag),
                active);
    }

    static Challenge challenge(String slug, Area area, Level level, boolean active, String tag) {
        return new Challenge(
                UUID.randomUUID(),
                slug,
                "Desafio " + slug,
                area.id(),
                level,
                Difficulty.EASY,
                "Enunciado",
                "Abordagem",
                new ExecutionLimits(5000, 262144),
                ComparisonMode.EXACT,
                5,
                Map.of(
                        Language.JAVA,
                        new ChallengeLanguage(Language.JAVA, "f", "class Solution {}", "ref")),
                List.of(
                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                Set.of(tag),
                active);
    }
}
