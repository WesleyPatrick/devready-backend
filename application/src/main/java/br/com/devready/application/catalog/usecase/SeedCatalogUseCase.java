package br.com.devready.application.catalog.usecase;

import br.com.devready.application.catalog.dto.AreaSeed;
import br.com.devready.application.catalog.dto.CatalogSeed;
import br.com.devready.application.catalog.dto.ChallengeSeed;
import br.com.devready.application.catalog.dto.QuestionSeed;
import br.com.devready.application.catalog.dto.SeedReport;
import br.com.devready.application.catalog.dto.TagSeed;
import br.com.devready.application.catalog.exception.InvalidSeedException;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.CatalogSeedSource;
import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.application.catalog.port.TagRepository;
import br.com.devready.application.shared.IdGenerator;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SeedCatalogUseCase {

    private final CatalogSeedSource source;
    private final AreaRepository areas;
    private final TagRepository tags;
    private final QuestionRepository questions;
    private final ChallengeRepository challenges;
    private final IdGenerator ids;

    public SeedCatalogUseCase(
            CatalogSeedSource source,
            AreaRepository areas,
            TagRepository tags,
            QuestionRepository questions,
            ChallengeRepository challenges,
            IdGenerator ids) {
        this.source = source;
        this.areas = areas;
        this.tags = tags;
        this.questions = questions;
        this.challenges = challenges;
        this.ids = ids;
    }

    public SeedReport execute() {
        CatalogSeed seed = source.load();
        Map<String, Area> areasBySlug = new HashMap<>();
        int insertedAreas = 0;
        for (AreaSeed areaSeed : seed.areas()) {
            Area area = areas.findBySlug(areaSeed.slug()).orElse(null);
            if (area == null) {
                area =
                        areas.save(
                                new Area(
                                        ids.next(),
                                        areaSeed.slug(),
                                        areaSeed.name(),
                                        new SessionConfig(
                                                areaSeed.questionCount(),
                                                areaSeed.challengeCount(),
                                                areaSeed.durationMinutes(),
                                                areaSeed.objectiveWeight()),
                                        true));
                insertedAreas++;
            }
            areasBySlug.put(area.slug(), area);
        }

        Set<String> knownTags = new HashSet<>();
        int insertedTags = 0;
        for (TagSeed tagSeed : seed.tags()) {
            if (tags.findBySlug(tagSeed.slug()).isEmpty()) {
                tags.save(new Tag(ids.next(), tagSeed.slug(), tagSeed.name()));
                insertedTags++;
            }
            knownTags.add(tagSeed.slug());
        }

        int insertedQuestions = 0;
        int updatedQuestions = 0;
        for (QuestionSeed q : seed.questions()) {
            Optional<Question> existing = questions.findBySlug(q.slug());
            if (existing.isPresent()) {
                if (refreshDisplay(existing.get(), q)) {
                    updatedQuestions++;
                }
                continue;
            }
            Area area = requireArea(areasBySlug, q.areaSlug(), q.slug());
            requireTags(knownTags, q.tags(), q.slug());
            List<QuestionOption> options = new ArrayList<>();
            for (int i = 0; i < q.options().size(); i++) {
                QuestionSeed.OptionSeed option = q.options().get(i);
                options.add(new QuestionOption(ids.next(), option.text(), option.correct(), i));
            }
            questions.save(
                    new Question(
                            ids.next(),
                            q.slug(),
                            q.title(),
                            area.id(),
                            q.level(),
                            q.difficulty(),
                            q.statement(),
                            q.explanation(),
                            options,
                            Set.copyOf(q.tags()),
                            true));
            insertedQuestions++;
        }

        int insertedChallenges = 0;
        int updatedChallenges = 0;
        for (ChallengeSeed c : seed.challenges()) {
            Optional<Challenge> existing = challenges.findBySlug(c.slug());
            if (existing.isPresent()) {
                if (refreshDisplay(existing.get(), c)) {
                    updatedChallenges++;
                }
                continue;
            }
            Area area = requireArea(areasBySlug, c.areaSlug(), c.slug());
            requireTags(knownTags, c.tags(), c.slug());
            Map<Language, ChallengeLanguage> languages = new HashMap<>();
            c.languages()
                    .forEach(
                            (language, entry) ->
                                    languages.put(
                                            language,
                                            new ChallengeLanguage(
                                                    language,
                                                    entry.functionName(),
                                                    entry.starterCode(),
                                                    entry.referenceSolution())));
            List<TestCase> cases = new ArrayList<>();
            for (int i = 0; i < c.testCases().size(); i++) {
                ChallengeSeed.TestCaseSeed tc = c.testCases().get(i);
                cases.add(
                        new TestCase(
                                ids.next(), i, tc.argsJson(), tc.expectedJson(), tc.visible()));
            }
            challenges.save(
                    new Challenge(
                            ids.next(),
                            c.slug(),
                            c.title(),
                            area.id(),
                            c.level(),
                            c.difficulty(),
                            c.statement(),
                            c.approachSummary(),
                            new ExecutionLimits(c.timeLimitMs(), c.memoryLimitKb()),
                            c.comparison(),
                            c.maxSubmissions(),
                            languages,
                            cases,
                            Set.copyOf(c.tags()),
                            true));
            insertedChallenges++;
        }

        return new SeedReport(
                insertedAreas,
                insertedTags,
                insertedQuestions,
                insertedChallenges,
                updatedQuestions,
                updatedChallenges);
    }

    private boolean refreshDisplay(Question current, QuestionSeed wanted) {
        if (current.title().equals(wanted.title()) && current.difficulty() == wanted.difficulty()) {
            return false;
        }
        Question refreshed =
                new Question(
                        current.id(),
                        current.slug(),
                        wanted.title(),
                        current.areaId(),
                        current.level(),
                        wanted.difficulty(),
                        current.statement(),
                        current.explanation(),
                        current.options(),
                        current.tagSlugs(),
                        current.active());
        questions.updateDisplay(refreshed.id(), refreshed.title(), refreshed.difficulty());
        return true;
    }

    private boolean refreshDisplay(Challenge current, ChallengeSeed wanted) {
        if (current.title().equals(wanted.title())
                && current.difficulty() == wanted.difficulty()
                && current.maxSubmissions() == wanted.maxSubmissions()) {
            return false;
        }
        Challenge refreshed =
                new Challenge(
                        current.id(),
                        current.slug(),
                        wanted.title(),
                        current.areaId(),
                        current.level(),
                        wanted.difficulty(),
                        current.statement(),
                        current.approachSummary(),
                        current.limits(),
                        current.comparisonMode(),
                        wanted.maxSubmissions(),
                        current.languages(),
                        current.testCases(),
                        current.tagSlugs(),
                        current.active());
        challenges.updateDisplay(
                refreshed.id(),
                refreshed.title(),
                refreshed.difficulty(),
                refreshed.maxSubmissions());
        return true;
    }

    private Area requireArea(Map<String, Area> areasBySlug, String areaSlug, String itemSlug) {
        Area area = areasBySlug.get(areaSlug);
        if (area == null) {
            area =
                    areas.findBySlug(areaSlug)
                            .orElseThrow(
                                    () ->
                                            new InvalidSeedException(
                                                    "Item "
                                                            + itemSlug
                                                            + " referencia a área desconhecida "
                                                            + areaSlug));
            areasBySlug.put(areaSlug, area);
        }
        return area;
    }

    private void requireTags(Set<String> knownTags, List<String> wanted, String itemSlug) {
        for (String tag : wanted) {
            if (!knownTags.contains(tag) && tags.findBySlug(tag).isEmpty()) {
                throw new InvalidSeedException(
                        "Item " + itemSlug + " usa a tag desconhecida " + tag);
            }
        }
    }
}
