package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.ChallengeLanguage;
import br.com.devready.domain.catalog.ExecutionLimits;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.catalog.Tag;
import br.com.devready.domain.catalog.TestCase;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class CatalogMappers {

    private CatalogMappers() {}

    static Area toDomain(AreaEntity e) {
        return new Area(
                e.getId(),
                e.getSlug(),
                e.getName(),
                new SessionConfig(
                        e.getQuestionCount(),
                        e.getChallengeCount(),
                        e.getDurationMinutes(),
                        e.getObjectiveWeight().doubleValue()),
                e.isActive());
    }

    static AreaEntity apply(Area area, AreaEntity e) {
        e.setSlug(area.slug());
        e.setName(area.name());
        e.setQuestionCount(area.sessionConfig().questionCount());
        e.setChallengeCount(area.sessionConfig().challengeCount());
        e.setDurationMinutes(area.sessionConfig().durationMinutes());
        e.setObjectiveWeight(BigDecimal.valueOf(area.sessionConfig().objectiveWeight()));
        e.setActive(area.active());
        return e;
    }

    static Tag toDomain(TagEntity e) {
        return new Tag(e.getId(), e.getSlug(), e.getName());
    }

    static Question toDomain(QuestionEntity e) {
        List<QuestionOption> options =
                e.getOptions().stream()
                        .map(
                                o ->
                                        new QuestionOption(
                                                o.getId(),
                                                o.getText(),
                                                o.isCorrect(),
                                                o.getPosition()))
                        .toList();
        return new Question(
                e.getId(),
                e.getSlug(),
                e.getTitle(),
                e.getAreaId(),
                e.getLevel(),
                e.getDifficulty(),
                e.getStatement(),
                e.getExplanation(),
                options,
                tagSlugs(e.getTags()),
                e.isActive());
    }

    static QuestionEntity apply(Question q, QuestionEntity e, Set<TagEntity> tags) {
        e.setSlug(q.slug());
        e.setTitle(q.title());
        e.setAreaId(q.areaId());
        e.setLevel(q.level());
        e.setDifficulty(q.difficulty());
        e.setStatement(q.statement());
        e.setExplanation(q.explanation());
        e.setActive(q.active());
        e.getOptions().clear();
        q.options()
                .forEach(
                        o ->
                                e.getOptions()
                                        .add(
                                                new QuestionOptionEntity(
                                                        o.id() != null ? o.id() : UUID.randomUUID(),
                                                        e,
                                                        o.text(),
                                                        o.correct(),
                                                        o.position())));
        e.getTags().clear();
        e.getTags().addAll(tags);
        return e;
    }

    static Challenge toDomain(ChallengeEntity e) {
        Map<Language, ChallengeLanguage> languages = new HashMap<>();
        e.getLanguages()
                .forEach(
                        l ->
                                languages.put(
                                        l.getLanguage(),
                                        new ChallengeLanguage(
                                                l.getLanguage(),
                                                l.getFunctionName(),
                                                l.getStarterCode(),
                                                l.getReferenceSolution())));
        List<TestCase> cases =
                e.getTestCases().stream()
                        .map(
                                t ->
                                        new TestCase(
                                                t.getId(),
                                                t.getPosition(),
                                                t.getArgsJson(),
                                                t.getExpectedJson(),
                                                t.isVisible()))
                        .toList();
        return new Challenge(
                e.getId(),
                e.getSlug(),
                e.getTitle(),
                e.getAreaId(),
                e.getLevel(),
                e.getDifficulty(),
                e.getStatement(),
                e.getApproachSummary(),
                new ExecutionLimits(e.getTimeLimitMs(), e.getMemoryLimitKb()),
                e.getComparisonMode(),
                e.getMaxSubmissions(),
                languages,
                cases,
                tagSlugs(e.getTags()),
                e.isActive());
    }

    static ChallengeEntity apply(Challenge c, ChallengeEntity e, Set<TagEntity> tags) {
        e.setSlug(c.slug());
        e.setTitle(c.title());
        e.setAreaId(c.areaId());
        e.setLevel(c.level());
        e.setDifficulty(c.difficulty());
        e.setStatement(c.statement());
        e.setApproachSummary(c.approachSummary());
        e.setTimeLimitMs(c.limits().timeLimitMs());
        e.setMemoryLimitKb(c.limits().memoryLimitKb());
        e.setComparisonMode(c.comparisonMode());
        e.setMaxSubmissions(c.maxSubmissions());
        e.setActive(c.active());
        e.getLanguages().clear();
        c.languages()
                .values()
                .forEach(
                        l ->
                                e.getLanguages()
                                        .add(
                                                new ChallengeLanguageEntity(
                                                        UUID.randomUUID(),
                                                        e,
                                                        l.language(),
                                                        l.functionName(),
                                                        l.starterCode(),
                                                        l.referenceSolution())));
        e.getTestCases().clear();
        c.testCases()
                .forEach(
                        t ->
                                e.getTestCases()
                                        .add(
                                                new ChallengeTestCaseEntity(
                                                        t.id() != null ? t.id() : UUID.randomUUID(),
                                                        e,
                                                        t.position(),
                                                        t.argsJson(),
                                                        t.expectedJson(),
                                                        t.visible())));
        e.getTags().clear();
        e.getTags().addAll(tags);
        return e;
    }

    static Set<String> tagSlugs(Set<TagEntity> tags) {
        return tags.stream().map(TagEntity::getSlug).collect(Collectors.toSet());
    }
}
