package br.com.devready.infrastructure.catalog.seed;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.career.dto.CareerSeed;
import br.com.devready.application.career.dto.CompanySeed;
import br.com.devready.application.career.dto.CompetencySeed;
import br.com.devready.application.career.dto.MaterialSeed;
import br.com.devready.application.career.dto.StackSeed;
import br.com.devready.application.career.dto.TrackSeed;
import br.com.devready.application.career.dto.TrackUnitSeed;
import br.com.devready.application.catalog.dto.CatalogSeed;
import br.com.devready.application.catalog.dto.ChallengeSeed;
import br.com.devready.application.catalog.dto.QuestionSeed;
import br.com.devready.application.catalog.dto.TagSeed;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.career.CompetencyCategory;
import br.com.devready.domain.career.Material;
import br.com.devready.domain.career.MaterialType;
import br.com.devready.domain.career.Requirement;
import br.com.devready.domain.career.Stack;
import br.com.devready.domain.career.Track;
import br.com.devready.domain.career.TrackUnit;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.ChallengeLanguage;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.ExecutionLimits;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.infrastructure.career.seed.YamlCareerSeedSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class SeedContentTest {

    private static CatalogSeed seed;
    private static CareerSeed career;
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Pattern DATE = Pattern.compile("\\b(19|20)\\d{2}-\\d{2}-\\d{2}\\b");

    private static final Pattern NEWER_THAN_JAVA_13 =
            Pattern.compile(
                    "case [^:\\n]*->"
                            + "|\\brecord\\s+[A-Z]"
                            + "|\"\"\""
                            + "|\\binstanceof\\s+[\\w.<>?,\\[\\] ]+\\s+[a-z]\\w*\\s*[)&|;]"
                            + "|(?<!Collectors)\\.toList\\(\\)");

    private static final Map<String, Long> QUESTIONS_PER_LEVEL =
            Map.of("backend", 12L, "frontend", 12L, "devops", 15L);

    private static final Map<String, Long> CHALLENGES_PER_LEVEL =
            Map.of("backend", 4L, "frontend", 4L, "devops", 2L);

    @BeforeAll
    static void load() {
        seed = new YamlCatalogSeedSource("classpath:seed").load();
        career = new YamlCareerSeedSource("classpath:seed").load();
    }

    @Test
    void theJavaCodeOfTheChallengesRunsOnJava13() {
        assertThat(NEWER_THAN_JAVA_13.matcher("case \"[ERROR]\" -> counts[0]++;").find())
                .as("a checagem pega o switch de seta")
                .isTrue();
        for (ChallengeSeed c : seed.challenges()) {
            ChallengeSeed.LanguageSeed java = c.languages().get(Language.JAVA);
            for (String code : List.of(java.starterCode(), java.referenceSolution())) {
                assertThat(NEWER_THAN_JAVA_13.matcher(code).find())
                        .as(c.slug() + " usa recurso de Java posterior ao 13, que o Judge0 não tem")
                        .isFalse();
            }
        }
    }

    @Test
    void hasThreeAreasAndEveryTagReferencedExists() {
        assertThat(seed.areas())
                .extracting(a -> a.slug())
                .containsExactlyInAnyOrder("backend", "frontend", "devops");
        Set<String> tags = seed.tags().stream().map(t -> t.slug()).collect(Collectors.toSet());
        assertThat(tags).hasSizeGreaterThanOrEqualTo(30);
        seed.questions().forEach(q -> assertThat(tags).as(q.slug()).containsAll(q.tags()));
        seed.challenges().forEach(c -> assertThat(tags).as(c.slug()).containsAll(c.tags()));
    }

    @Test
    void questionCountsMeetTheMinimumsInEveryAreaAndLevel() {
        Map<String, Long> perBatch =
                seed.questions().stream()
                        .collect(
                                Collectors.groupingBy(
                                        q -> q.areaSlug() + "/" + q.level(),
                                        Collectors.counting()));
        QUESTIONS_PER_LEVEL.forEach(
                (area, minimum) -> {
                    for (Level level : Level.values()) {
                        assertThat(perBatch.getOrDefault(area + "/" + level, 0L))
                                .as("objetivas de " + area + " " + level)
                                .isGreaterThanOrEqualTo(minimum);
                    }
                });
    }

    @Test
    void challengeCountsMeetTheMinimumsInEveryAreaAndLevel() {
        Map<String, Long> perBatch =
                seed.challenges().stream()
                        .collect(
                                Collectors.groupingBy(
                                        c -> c.areaSlug() + "/" + c.level(),
                                        Collectors.counting()));
        CHALLENGES_PER_LEVEL.forEach(
                (area, minimum) -> {
                    for (Level level : Level.values()) {
                        assertThat(perBatch.getOrDefault(area + "/" + level, 0L))
                                .as("desafios de " + area + " " + level)
                                .isGreaterThanOrEqualTo(minimum);
                    }
                });
    }

    @Test
    void theCorrectOptionIsTheLongestInAtMostAThirdOfEachPlenoAndSeniorBatch() {
        Map<String, List<QuestionSeed>> batches =
                seed.questions().stream()
                        .filter(q -> q.level() != Level.JUNIOR)
                        .collect(Collectors.groupingBy(q -> q.areaSlug() + "/" + q.level()));
        assertThat(batches).as("áreas Pleno e Sênior").hasSize(6);
        batches.forEach(
                (batch, questions) -> {
                    long longest =
                            questions.stream()
                                    .filter(SeedContentTest::correctIsLongestOrTied)
                                    .count();
                    assertThat(longest * 3)
                            .as(
                                    batch
                                            + ": a certa é a mais longa ou empata em "
                                            + longest
                                            + " de "
                                            + questions.size())
                            .isLessThanOrEqualTo(questions.size());
                });
    }

    @Test
    void everyQuestionSlugStartsWithItsAreaAndLevel() {
        for (QuestionSeed q : seed.questions()) {
            assertThat(q.slug())
                    .as(q.slug())
                    .startsWith(
                            q.areaSlug() + "-" + q.level().name().toLowerCase(Locale.ROOT) + "-");
        }
    }

    @Test
    void everyQuestionAndChallengeHasATagOfACompetencyInATrackOfItsArea() {
        Map<String, List<String>> tagsByCompetency =
                career.competencies().stream()
                        .collect(Collectors.toMap(CompetencySeed::slug, CompetencySeed::tags));
        Map<String, Set<String>> tagsByArea = new HashMap<>();
        for (TrackSeed t : career.tracks()) {
            for (TrackUnitSeed u : t.units()) {
                tagsByArea
                        .computeIfAbsent(t.area(), area -> new HashSet<>())
                        .addAll(tagsByCompetency.getOrDefault(u.competency(), List.of()));
            }
        }
        for (QuestionSeed q : seed.questions()) {
            assertThat(q.tags())
                    .as(q.slug() + " sem tag de competência das trilhas de " + q.areaSlug())
                    .containsAnyElementsOf(tagsByArea.getOrDefault(q.areaSlug(), Set.of()));
        }
        for (ChallengeSeed c : seed.challenges()) {
            assertThat(c.tags())
                    .as(c.slug() + " sem tag de competência das trilhas de " + c.areaSlug())
                    .containsAnyElementsOf(tagsByArea.getOrDefault(c.areaSlug(), Set.of()));
        }
    }

    private static boolean correctIsLongestOrTied(QuestionSeed q) {
        int correct = 0;
        int longestWrong = 0;
        for (QuestionSeed.OptionSeed option : q.options()) {
            int length = option.text().strip().length();
            if (option.correct()) {
                correct = length;
            } else {
                longestWrong = Math.max(longestWrong, length);
            }
        }
        return correct >= longestWrong;
    }

    @Test
    void slugsAreUnique() {
        List<String> slugs = new ArrayList<>();
        seed.questions().forEach(q -> slugs.add(q.slug()));
        seed.challenges().forEach(c -> slugs.add(c.slug()));
        assertThat(slugs).doesNotHaveDuplicates();
    }

    @Test
    void everyQuestionIsValidWellWrittenAndVaried() {
        Map<Integer, Integer> correctPositions = new HashMap<>();
        for (QuestionSeed q : seed.questions()) {
            List<QuestionOption> options = new ArrayList<>();
            for (int i = 0; i < q.options().size(); i++) {
                options.add(
                        new QuestionOption(
                                UUID.randomUUID(),
                                q.options().get(i).text(),
                                q.options().get(i).correct(),
                                i));
            }
            Question question =
                    new Question(
                            UUID.randomUUID(),
                            q.slug(),
                            q.title(),
                            UUID.randomUUID(),
                            q.level(),
                            q.difficulty(),
                            q.statement(),
                            q.explanation(),
                            options,
                            Set.copyOf(q.tags()),
                            true);
            assertThat(question.options()).as(q.slug()).hasSize(4);
            assertThat(q.options().stream().map(o -> o.text().strip()).collect(Collectors.toSet()))
                    .as(q.slug())
                    .hasSize(4);
            assertThat(q.statement().strip().length())
                    .as(q.slug() + " enunciado curto")
                    .isGreaterThanOrEqualTo(40);
            assertThat(q.explanation().strip().length())
                    .as(q.slug() + " explicação curta")
                    .isGreaterThanOrEqualTo(60);
            assertThat(q.tags()).as(q.slug()).hasSizeBetween(1, 3);
            assertThat(DATE.matcher(q.statement() + q.explanation()).find())
                    .as(q.slug() + " tem data")
                    .isFalse();
            correctPositions.merge(question.correctOption().position(), 1, Integer::sum);
            int correctLength = question.correctOption().text().strip().length();
            int longestWrong =
                    question.options().stream()
                            .filter(o -> !o.correct())
                            .mapToInt(o -> o.text().strip().length())
                            .max()
                            .orElse(0);
            assertThat(correctLength)
                    .as(q.slug() + " correta muito mais longa que as erradas")
                    .isLessThanOrEqualTo((int) Math.ceil(longestWrong * 1.3));
        }
        assertThat(correctPositions.keySet())
                .as("posição da correta varia")
                .hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void everyChallengeHasThreeLanguagesReferenceSolutionsAndValidJsonCases() {
        for (ChallengeSeed c : seed.challenges()) {
            assertThat(c.languages().keySet())
                    .as(c.slug())
                    .containsExactlyInAnyOrder(Language.JAVA, Language.PYTHON, Language.JAVASCRIPT);
            Map<Language, ChallengeLanguage> languages = new HashMap<>();
            c.languages()
                    .forEach(
                            (lang, entry) -> {
                                assertThat(entry.referenceSolution())
                                        .as(c.slug() + " " + lang + " sem solução de referência")
                                        .isNotBlank();
                                assertThat(entry.starterCode())
                                        .as(c.slug() + " " + lang)
                                        .contains("Solution")
                                        .contains(entry.functionName());
                                assertThat(entry.referenceSolution())
                                        .as(c.slug() + " " + lang)
                                        .contains(entry.functionName());
                                languages.put(
                                        lang,
                                        new ChallengeLanguage(
                                                lang,
                                                entry.functionName(),
                                                entry.starterCode(),
                                                entry.referenceSolution()));
                            });
            assertThat(languages.get(Language.PYTHON).functionName())
                    .as(c.slug())
                    .matches("[a-z][a-z0-9_]*");
            assertThat(languages.get(Language.JAVA).functionName())
                    .as(c.slug())
                    .matches("[a-z][A-Za-z0-9]*");
            List<TestCase> cases = new ArrayList<>();
            for (int i = 0; i < c.testCases().size(); i++) {
                var tc = c.testCases().get(i);
                JSON.readTree(tc.argsJson());
                JSON.readTree(tc.expectedJson());
                assertThat(JSON.readTree(tc.argsJson()).isArray())
                        .as(c.slug() + " args deve ser lista")
                        .isTrue();
                cases.add(
                        new TestCase(
                                UUID.randomUUID(),
                                i,
                                tc.argsJson(),
                                tc.expectedJson(),
                                tc.visible()));
            }
            assertThat(cases).as(c.slug()).hasSizeBetween(5, 8);
            Challenge challenge =
                    new Challenge(
                            UUID.randomUUID(),
                            c.slug(),
                            c.title(),
                            UUID.randomUUID(),
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
                            true);
            assertThat(challenge.visibleTestCases()).as(c.slug()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(challenge.hiddenTestCases()).as(c.slug()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(c.approachSummary().strip().length())
                    .as(c.slug() + " abordagem curta")
                    .isGreaterThanOrEqualTo(80);
            assertThat(c.statement().strip().length())
                    .as(c.slug() + " enunciado curto")
                    .isGreaterThanOrEqualTo(80);
            assertThat(c.tags()).as(c.slug()).hasSizeBetween(1, 3);
            assertThat(DATE.matcher(c.statement() + c.approachSummary()).find())
                    .as(c.slug() + " tem data")
                    .isFalse();
        }
    }

    @Test
    void referenceSolutionsAreNotJustTheStarterCode() {
        for (ChallengeSeed c : seed.challenges()) {
            c.languages()
                    .forEach(
                            (lang, entry) ->
                                    assertThat(entry.referenceSolution().strip())
                                            .as(c.slug() + " " + lang)
                                            .isNotEqualTo(entry.starterCode().strip()));
        }
        Set<String> seen = new HashSet<>();
        seed.challenges()
                .forEach(
                        c ->
                                assertThat(seen.add(c.title()))
                                        .as("título repetido: " + c.title())
                                        .isTrue());
    }

    @Test
    void companiesAreTheTenOfTheResearchFiveInEachGroup() {
        assertThat(career.companies()).hasSize(10);
        assertThat(career.companies()).extracting(CompanySeed::slug).doesNotHaveDuplicates();
        Map<String, Long> perGroup =
                career.companies().stream()
                        .collect(Collectors.groupingBy(CompanySeed::group, Collectors.counting()));
        assertThat(perGroup)
                .containsOnlyKeys("big-tech", "tech-brasil")
                .containsEntry("big-tech", 5L)
                .containsEntry("tech-brasil", 5L);
        for (CompanySeed c : career.companies()) {
            new Company(
                    UUID.randomUUID(),
                    c.slug(),
                    c.name(),
                    CompanyGroup.fromSlug(c.group()),
                    c.about(),
                    c.careers(),
                    true);
            assertThat(c.careers()).as(c.slug() + " sem página de carreira").isNotEmpty();
            c.careers().forEach(url -> assertThat(url).as(c.slug()).startsWith("https://"));
        }
    }

    @Test
    void companiesAndStacksFitTheirColumns() {
        for (CompanySeed c : career.companies()) {
            assertThat(c.slug().length()).as(c.slug() + " slug").isLessThanOrEqualTo(50);
            assertThat(c.name().length()).as(c.slug() + " nome").isLessThanOrEqualTo(80);
            c.careers()
                    .forEach(
                            url ->
                                    assertThat(url.length())
                                            .as(c.slug() + " " + url)
                                            .isLessThanOrEqualTo(500));
        }
        for (StackSeed s : career.stacks()) {
            assertThat(s.slug().length()).as(s.slug() + " slug").isLessThanOrEqualTo(50);
            assertThat(s.name().length()).as(s.slug() + " nome").isLessThanOrEqualTo(80);
        }
    }

    @Test
    void stacksHaveUniqueSlugsAndPointToKnownAreas() {
        Set<String> areas = seed.areas().stream().map(a -> a.slug()).collect(Collectors.toSet());
        assertThat(career.stacks()).isNotEmpty();
        assertThat(career.stacks()).extracting(StackSeed::slug).doesNotHaveDuplicates();
        for (StackSeed s : career.stacks()) {
            new Stack(UUID.randomUUID(), s.slug(), s.name(), s.area());
            assertThat(areas).as(s.slug() + " aponta para área desconhecida").contains(s.area());
        }
    }

    @Test
    void everyQuestionHasAShortUniqueTitleAndEveryItemADifficulty() {
        Set<String> titles = new HashSet<>();
        for (QuestionSeed q : seed.questions()) {
            assertThat(q.title()).as(q.slug() + " sem título").isNotBlank();
            assertThat(q.title().length()).as(q.slug() + " título longo").isLessThanOrEqualTo(60);
            assertThat(q.title())
                    .as(q.slug() + " título que pergunta ou corta")
                    .doesNotEndWith("?")
                    .doesNotEndWith("…");
            assertThat(titles.add(q.title())).as("título repetido: " + q.title()).isTrue();
            assertThat(q.difficulty()).as(q.slug() + " sem dificuldade").isNotNull();
        }
        seed.challenges()
                .forEach(
                        c ->
                                assertThat(c.difficulty())
                                        .as(c.slug() + " sem dificuldade")
                                        .isNotNull());
    }

    @Test
    void competenciesHaveUniqueSlugsSummariesAndKnownTags() {
        Set<String> tags = seed.tags().stream().map(TagSeed::slug).collect(Collectors.toSet());
        assertThat(career.competencies()).extracting(CompetencySeed::slug).doesNotHaveDuplicates();
        for (CompetencySeed c : career.competencies()) {
            Competency competency =
                    new Competency(
                            UUID.randomUUID(),
                            c.slug(),
                            c.name(),
                            CompetencyCategory.fromSlug(c.category()),
                            c.summary(),
                            Difficulty.fromSlug(c.difficulty()),
                            Set.copyOf(c.tags()),
                            List.of());
            assertThat(competency.summary()).as(c.slug() + " sem resumo").isNotBlank();
            assertThat(c.slug().length()).as(c.slug() + " slug").isLessThanOrEqualTo(60);
            assertThat(c.name().length()).as(c.slug() + " nome").isLessThanOrEqualTo(80);
            assertThat(tags).as(c.slug() + " tag desconhecida").containsAll(c.tags());
            assertThat(DATE.matcher(c.summary()).find()).as(c.slug() + " tem data").isFalse();
        }
    }

    @Test
    void materialsPointToKnownCompetenciesAndFitTheirColumns() {
        Set<String> competencies =
                career.competencies().stream()
                        .map(CompetencySeed::slug)
                        .collect(Collectors.toSet());
        assertThat(career.materials()).isNotEmpty();
        for (MaterialSeed m : career.materials()) {
            new Material(
                    m.title(), m.url(), MaterialType.fromSlug(m.type()), m.language(), m.free());
            assertThat(competencies).as(m.title()).contains(m.competency());
            assertThat(m.url()).as(m.title()).startsWith("https://");
            assertThat(m.title().length()).as(m.title()).isLessThanOrEqualTo(200);
            assertThat(m.url().length()).as(m.title()).isLessThanOrEqualTo(500);
        }
    }

    @Test
    void tracksPointToKnownCompaniesAreasStacksAndCompetencies() {
        Set<String> companies =
                career.companies().stream().map(CompanySeed::slug).collect(Collectors.toSet());
        Set<String> areas = seed.areas().stream().map(a -> a.slug()).collect(Collectors.toSet());
        Map<String, String> stackArea =
                career.stacks().stream()
                        .collect(Collectors.toMap(StackSeed::slug, StackSeed::area));
        Set<String> competencies =
                career.competencies().stream()
                        .map(CompetencySeed::slug)
                        .collect(Collectors.toSet());
        Set<String> keys = new HashSet<>();
        for (TrackSeed t : career.tracks()) {
            String key = t.company() + "/" + t.area() + "/" + t.seniority() + "/" + t.stack();
            assertThat(keys.add(key)).as("trilha repetida: " + key).isTrue();
            assertThat(companies).as(key).contains(t.company());
            assertThat(areas).as(key).contains(t.area());
            assertThat(List.of("todas", "junior", "pleno", "senior"))
                    .as(key)
                    .contains(t.seniority());
            if (!"todas".equals(t.stack())) {
                assertThat(stackArea.get(t.stack()))
                        .as(key + " stack de outra área")
                        .isEqualTo(t.area());
            }
            List<TrackUnit> units = new ArrayList<>();
            for (TrackUnitSeed u : t.units()) {
                assertThat(competencies).as(key + " " + u.competency()).contains(u.competency());
                units.add(
                        new TrackUnit(
                                u.competency(),
                                u.weight(),
                                Requirement.fromSlug(u.requirement()),
                                u.jobs()));
            }
            new Track(
                    UUID.randomUUID(),
                    t.company(),
                    t.area(),
                    "todas".equals(t.stack()) ? null : t.stack(),
                    null,
                    t.jobs(),
                    units);
        }
    }

    @Test
    void careerSeedKeepsTheJobCountsOfTheFirstRound() {
        assertThat(career.competencies()).hasSize(23);
        assertThat(career.tracks()).hasSize(29);
        for (TrackSeed t : career.tracks()) {
            for (TrackUnitSeed u : t.units()) {
                assertThat(u.jobs())
                        .as(t.company() + "/" + t.area() + " " + u.competency())
                        .isEqualTo((int) Math.round(u.weight() * t.jobs()));
            }
        }
        Map<String, Long> perCompetency =
                career.materials().stream()
                        .collect(
                                Collectors.groupingBy(
                                        MaterialSeed::competency, Collectors.counting()));
        for (CompetencySeed c : career.competencies()) {
            assertThat(perCompetency.getOrDefault(c.slug(), 0L))
                    .as(c.slug() + " materiais")
                    .isBetween(1L, 3L);
        }
        assertThat(jobsOf("amazon", "backend", "todas", "todas")).isEqualTo(205);
        assertThat(jobsOf("amazon", "backend", "todas", "junior")).isEqualTo(8);
        assertThat(jobsOf("nubank", "backend", "todas", "todas")).isEqualTo(17);
        assertThat(jobsOf("google", "devops", "todas", "todas")).isEqualTo(10);
        assertThat(career.tracks()).noneMatch(t -> t.company().equals("meta"));
    }

    private static int jobsOf(String company, String area, String stack, String seniority) {
        return career.tracks().stream()
                .filter(
                        t ->
                                t.company().equals(company)
                                        && t.area().equals(area)
                                        && t.stack().equals(stack)
                                        && t.seniority().equals(seniority))
                .findFirst()
                .orElseThrow()
                .jobs();
    }
}
