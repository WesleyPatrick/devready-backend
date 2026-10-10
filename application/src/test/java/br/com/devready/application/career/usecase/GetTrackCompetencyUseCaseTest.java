package br.com.devready.application.career.usecase;

import static br.com.devready.application.career.fake.CareerFixtures.company;
import static br.com.devready.application.career.fake.CareerFixtures.competency;
import static br.com.devready.application.career.fake.CareerFixtures.stack;
import static br.com.devready.application.career.fake.CareerFixtures.track;
import static br.com.devready.application.career.fake.CareerFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.dto.TrackChallengeView;
import br.com.devready.application.career.dto.TrackCompetencyDetail;
import br.com.devready.application.career.dto.TrackQuestionView;
import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.career.exception.CompetencyNotInTrackException;
import br.com.devready.application.career.fake.InMemoryCareerProfileRepository;
import br.com.devready.application.career.fake.InMemoryCompanyRepository;
import br.com.devready.application.career.fake.InMemoryCompetencyRepository;
import br.com.devready.application.career.fake.InMemoryQuestionProgressReader;
import br.com.devready.application.career.fake.InMemoryStackRepository;
import br.com.devready.application.career.fake.InMemoryTrackRepository;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.catalog.dto.ChallengeSummary;
import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.application.catalog.dto.QuestionSummary;
import br.com.devready.application.catalog.fake.InMemoryAreaRepository;
import br.com.devready.application.catalog.fake.InMemoryChallengeRepository;
import br.com.devready.application.catalog.fake.InMemoryPracticeCatalog;
import br.com.devready.application.catalog.fake.InMemoryQuestionRepository;
import br.com.devready.application.catalog.port.PracticeCatalog;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.CompetencyCategory;
import br.com.devready.domain.career.CompetencyStatus;
import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.career.Requirement;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetTrackCompetencyUseCaseTest {

    private final InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
    private final InMemoryStackRepository stacks = new InMemoryStackRepository();
    private final InMemoryCareerProfileRepository profiles = new InMemoryCareerProfileRepository();
    private final InMemoryCompetencyRepository competencies = new InMemoryCompetencyRepository();
    private final InMemoryTrackRepository tracks = new InMemoryTrackRepository();
    private final InMemoryAreaRepository areas = new InMemoryAreaRepository();
    private final InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
    private final InMemoryChallengeRepository challenges = new InMemoryChallengeRepository();
    private final InMemoryQuestionProgressReader progress = new InMemoryQuestionProgressReader();
    private final GetTrackCompetencyUseCase getCompetency =
            new GetTrackCompetencyUseCase(
                    new TrackAssembler(
                            profiles,
                            companies,
                            stacks,
                            competencies,
                            tracks,
                            areas,
                            new InMemoryPracticeCatalog(questions, challenges),
                            progress),
                    areas,
                    new InMemoryPracticeCatalog(questions, challenges));
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void seed() {
        Area backend = area("backend");
        Area frontend = area("frontend");
        companies.save(company("nubank", "Nubank", CompanyGroup.TECH_BRASIL));
        stacks.save(stack("java-spring", "Java e Spring", "backend"));
        competencies.save(
                competency(
                        "sql",
                        "SQL e modelagem de dados",
                        CompetencyCategory.FUNDAMENTO,
                        Difficulty.MEDIUM,
                        "sql",
                        "banco-de-dados"));
        competencies.save(
                competency(
                        "ingles", "Inglês", CompetencyCategory.COMPORTAMENTAL, Difficulty.MEDIUM));
        competencies.save(
                competency(
                        "react",
                        "React",
                        CompetencyCategory.FRAMEWORK,
                        Difficulty.MEDIUM,
                        "react"));
        tracks.save(
                track(
                        "nubank",
                        "backend",
                        null,
                        null,
                        17,
                        unit("sql", 0.41, Requirement.OBRIGATORIA, 7),
                        unit("ingles", 0.59, Requirement.DESEJAVEL, 10)));
        profiles.save(new CareerProfile(user, List.of("nubank"), "java-spring", Level.JUNIOR));
        questions.save(
                question(
                        "q-hard",
                        "Cem consultas",
                        backend,
                        Difficulty.HARD,
                        true,
                        "banco-de-dados"));
        questions.save(
                question("q-easy-b", "Pedidos com cliente", backend, Difficulty.EASY, true, "sql"));
        questions.save(
                question("q-easy-a", "chave estrangeira", backend, Difficulty.EASY, true, "sql"));
        questions.save(
                question(
                        "q-medium",
                        "Busca lenta",
                        backend,
                        Difficulty.MEDIUM,
                        true,
                        "sql",
                        "java"));
        questions.save(question("q-inativa", "Inativa", backend, Difficulty.EASY, false, "sql"));
        questions.save(
                question("q-frontend", "Outra área", frontend, Difficulty.EASY, true, "sql"));
        questions.save(question("q-http", "Outra tag", backend, Difficulty.EASY, true, "http"));
        challenges.save(challenge("c-medium", "Mesclar", backend, Difficulty.MEDIUM, "sql"));
        challenges.save(challenge("c-easy", "Soma", backend, Difficulty.EASY, "banco-de-dados"));
    }

    @Test
    void bringsTheCatalogItemsOfTheStackAreaOrderedByDifficultyAndTitle() {
        TrackCompetencyDetail detail = getCompetency.execute(user, "sql");

        assertThat(detail.competency().competency().slug()).isEqualTo("sql");
        assertThat(detail.competency().askedBy()).containsExactly("nubank");
        assertThat(detail.jobsAnalyzed()).isEqualTo(17);
        assertThat(detail.questions())
                .extracting(TrackQuestionView::slug)
                .containsExactly("q-easy-a", "q-easy-b", "q-medium", "q-hard");
        assertThat(detail.questions().get(0))
                .isEqualTo(
                        new TrackQuestionView(
                                "q-easy-a",
                                "chave estrangeira",
                                Difficulty.EASY,
                                Level.JUNIOR,
                                QuestionProgress.NOT_STARTED));
        assertThat(detail.challenges())
                .extracting(TrackChallengeView::slug)
                .containsExactly("c-easy", "c-medium");
        assertThat(detail.challenges().get(0).languages())
                .containsExactly(Language.JAVA, Language.PYTHON, Language.JAVASCRIPT);
    }

    @Test
    void questionsWithTheSameDifficultyAndTitleAreOrderedBySlug() {
        TrackCompetencyDetail detail =
                withCatalog(
                                List.of(
                                        new QuestionSummary(
                                                "q-b", "Junção", Difficulty.EASY, Level.JUNIOR),
                                        new QuestionSummary(
                                                "q-a", "junção", Difficulty.EASY, Level.JUNIOR)),
                                List.of())
                        .execute(user, "sql");

        assertThat(detail.questions())
                .extracting(TrackQuestionView::slug)
                .containsExactly("q-a", "q-b");
    }

    @Test
    void challengesWithTheSameDifficultyAndTitleAreOrderedBySlug() {
        TrackCompetencyDetail detail =
                withCatalog(
                                List.of(),
                                List.of(
                                        new ChallengeSummary(
                                                "c-b",
                                                "Soma",
                                                Difficulty.EASY,
                                                Level.JUNIOR,
                                                List.of(Language.JAVA)),
                                        new ChallengeSummary(
                                                "c-a",
                                                "soma",
                                                Difficulty.EASY,
                                                Level.JUNIOR,
                                                List.of(Language.JAVA))))
                        .execute(user, "sql");

        assertThat(detail.challenges())
                .extracting(TrackChallengeView::slug)
                .containsExactly("c-a", "c-b");
    }

    @Test
    void anAccentedTitleIsOrderedByItsBaseLetter() {
        TrackCompetencyDetail detail =
                withCatalog(
                                List.of(
                                        new QuestionSummary(
                                                "q-z", "Zebra", Difficulty.EASY, Level.JUNIOR),
                                        new QuestionSummary(
                                                "q-i",
                                                "Índice ou id como key",
                                                Difficulty.EASY,
                                                Level.JUNIOR),
                                        new QuestionSummary(
                                                "q-h", "Hash", Difficulty.EASY, Level.JUNIOR)),
                                List.of(
                                        new ChallengeSummary(
                                                "c-z",
                                                "Zebra",
                                                Difficulty.EASY,
                                                Level.JUNIOR,
                                                List.of(Language.JAVA)),
                                        new ChallengeSummary(
                                                "c-i",
                                                "Índice",
                                                Difficulty.EASY,
                                                Level.JUNIOR,
                                                List.of(Language.JAVA))))
                        .execute(user, "sql");

        assertThat(detail.questions())
                .extracting(TrackQuestionView::slug)
                .containsExactly("q-h", "q-i", "q-z");
        assertThat(detail.challenges())
                .extracting(TrackChallengeView::slug)
                .containsExactly("c-i", "c-z");
    }

    @Test
    void competencyWithoutTagsHasNoCatalogItems() {
        TrackCompetencyDetail detail = getCompetency.execute(user, "ingles");

        assertThat(detail.questions()).isEmpty();
        assertThat(detail.challenges()).isEmpty();
        assertThat(detail.competency().requirement()).isEqualTo(Requirement.DESEJAVEL);
    }

    @Test
    void competencyOutsideTheTrackIsNotFound() {
        assertThatThrownBy(() -> getCompetency.execute(user, "react"))
                .isInstanceOf(CompetencyNotInTrackException.class)
                .hasMessage("Competência fora da sua trilha: react.");
        assertThatThrownBy(() -> getCompetency.execute(user, "nao-existe"))
                .isInstanceOf(CompetencyNotInTrackException.class);
    }

    @Test
    void failsWithoutAProfile() {
        assertThatThrownBy(() -> getCompetency.execute(UUID.randomUUID(), "sql"))
                .isInstanceOf(CareerProfileNotFoundException.class);
    }

    @Test
    void eachQuestionCarriesThePersonsProgressAndTheCompetencyItsStatus() {
        progress.record(user, "q-easy-a", QuestionProgress.SOLVED);
        progress.record(user, "q-medium", QuestionProgress.ATTEMPTED);

        TrackCompetencyDetail detail = getCompetency.execute(user, "sql");

        assertThat(detail.questions())
                .extracting(TrackQuestionView::progress)
                .containsExactly(
                        QuestionProgress.SOLVED,
                        QuestionProgress.NOT_STARTED,
                        QuestionProgress.ATTEMPTED,
                        QuestionProgress.NOT_STARTED);
        assertThat(detail.competency().status()).isEqualTo(CompetencyStatus.IN_PROGRESS);
    }

    private GetTrackCompetencyUseCase withCatalog(
            List<QuestionSummary> questionList, List<ChallengeSummary> challengeList) {
        PracticeCatalog fixed =
                new PracticeCatalog() {
                    @Override
                    public List<QuestionSummary> activeQuestions(
                            UUID areaId, Set<String> tags, Set<Level> levels) {
                        return questionList;
                    }

                    @Override
                    public List<ChallengeSummary> activeChallenges(
                            UUID areaId, Set<String> tags, Set<Level> levels) {
                        return challengeList;
                    }

                    @Override
                    public List<ProblemSummary> activeProblems(UUID areaId, Set<Level> levels) {
                        return List.of();
                    }
                };
        return new GetTrackCompetencyUseCase(
                new TrackAssembler(
                        profiles, companies, stacks, competencies, tracks, areas, fixed, progress),
                areas,
                fixed);
    }

    private Area area(String slug) {
        return areas.save(
                new Area(UUID.randomUUID(), slug, slug, new SessionConfig(8, 2, 45, 0.5), true));
    }

    private static Question question(
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
                        new QuestionOption(UUID.randomUUID(), "A", true, 0),
                        new QuestionOption(UUID.randomUUID(), "B", false, 1)),
                Set.of(tags),
                active);
    }

    private static Challenge challenge(
            String slug, String title, Area area, Difficulty difficulty, String... tags) {
        Map<Language, ChallengeLanguage> languages = new LinkedHashMap<>();
        for (Language language : List.of(Language.JAVASCRIPT, Language.JAVA, Language.PYTHON)) {
            languages.put(
                    language, new ChallengeLanguage(language, "f", "class Solution {}", "ref"));
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
                languages,
                List.of(
                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                Set.of(tags),
                true);
    }

    @Test
    void eachChallengeComesWithTheProgressOfThePerson() {
        progress.record(user, "c-easy", QuestionProgress.SOLVED);
        progress.record(user, "c-medium", QuestionProgress.ATTEMPTED);

        TrackCompetencyDetail detail = getCompetency.execute(user, "sql");

        assertThat(detail.challenges())
                .extracting(TrackChallengeView::progress)
                .containsExactly(QuestionProgress.SOLVED, QuestionProgress.ATTEMPTED);
    }
}
