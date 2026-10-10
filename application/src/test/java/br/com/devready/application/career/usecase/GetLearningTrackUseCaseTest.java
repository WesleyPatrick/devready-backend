package br.com.devready.application.career.usecase;

import static br.com.devready.application.career.fake.CareerFixtures.company;
import static br.com.devready.application.career.fake.CareerFixtures.competency;
import static br.com.devready.application.career.fake.CareerFixtures.stack;
import static br.com.devready.application.career.fake.CareerFixtures.track;
import static br.com.devready.application.career.fake.CareerFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.dto.LearningTrackView;
import br.com.devready.application.career.dto.TrackCompetencyView;
import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.career.exception.UnknownStackException;
import br.com.devready.application.career.fake.InMemoryCareerProfileRepository;
import br.com.devready.application.career.fake.InMemoryCompanyRepository;
import br.com.devready.application.career.fake.InMemoryCompetencyRepository;
import br.com.devready.application.career.fake.InMemoryQuestionProgressReader;
import br.com.devready.application.career.fake.InMemoryStackRepository;
import br.com.devready.application.career.fake.InMemoryTrackRepository;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.catalog.fake.InMemoryAreaRepository;
import br.com.devready.application.catalog.fake.InMemoryChallengeRepository;
import br.com.devready.application.catalog.fake.InMemoryPracticeCatalog;
import br.com.devready.application.catalog.fake.InMemoryQuestionRepository;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.CompetencyCategory;
import br.com.devready.domain.career.CompetencyStatus;
import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.career.Requirement;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetLearningTrackUseCaseTest {

    private static final Requirement O = Requirement.OBRIGATORIA;
    private static final Requirement D = Requirement.DESEJAVEL;

    private final InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
    private final InMemoryStackRepository stacks = new InMemoryStackRepository();
    private final InMemoryCareerProfileRepository profiles = new InMemoryCareerProfileRepository();
    private final InMemoryCompetencyRepository competencies = new InMemoryCompetencyRepository();
    private final InMemoryTrackRepository tracks = new InMemoryTrackRepository();
    private final InMemoryAreaRepository areas = new InMemoryAreaRepository();
    private final InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
    private final InMemoryChallengeRepository challenges = new InMemoryChallengeRepository();
    private final InMemoryQuestionProgressReader progress = new InMemoryQuestionProgressReader();
    private final TrackAssembler assembler =
            new TrackAssembler(
                    profiles,
                    companies,
                    stacks,
                    competencies,
                    tracks,
                    areas,
                    new InMemoryPracticeCatalog(questions, challenges),
                    progress);
    private final GetLearningTrackUseCase getTrack = new GetLearningTrackUseCase(assembler);
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void seed() {
        companies.save(company("nubank", "Nubank", CompanyGroup.TECH_BRASIL));
        companies.save(company("google", "Google", CompanyGroup.BIG_TECH));
        companies.save(company("meta", "Meta", CompanyGroup.BIG_TECH));
        stacks.save(stack("java-spring", "Java e Spring", "backend"));
        stacks.save(stack("react", "React", "frontend"));
        competencies.save(
                competency(
                        "sql",
                        "SQL e modelagem de dados",
                        CompetencyCategory.FUNDAMENTO,
                        Difficulty.MEDIUM,
                        "sql"));
        competencies.save(
                competency(
                        "java", "Java", CompetencyCategory.LINGUAGEM, Difficulty.MEDIUM, "java"));
        competencies.save(
                competency("ingles", "Inglês", CompetencyCategory.COMPORTAMENTAL, Difficulty.EASY));
        tracks.save(
                track(
                        "nubank",
                        "backend",
                        null,
                        null,
                        17,
                        unit("sql", 0.41, O, 7),
                        unit("ingles", 0.59, O, 10)));
        tracks.save(
                track(
                        "google",
                        "backend",
                        "java-spring",
                        null,
                        9,
                        unit("java", 0.89, O, 8),
                        unit("sql", 0.44, D, 4)));
        tracks.save(track("google", "frontend", null, null, 3, unit("java", 0.33, D, 1)));
    }

    @Test
    void failsWithoutAProfile() {
        assertThatThrownBy(() -> getTrack.execute(user))
                .isInstanceOf(CareerProfileNotFoundException.class)
                .hasMessage("Perfil de carreira ainda não preenchido.");
    }

    @Test
    void fusesTheTracksOfTheChosenCompaniesInTheAreaOfTheStack() {
        profiles.save(
                new CareerProfile(
                        user, List.of("meta", "nubank", "google"), "java-spring", Level.JUNIOR));

        LearningTrackView view = getTrack.execute(user);

        assertThat(view.stack().slug()).isEqualTo("java-spring");
        assertThat(view.jobsAnalyzed()).isEqualTo(26);
        assertThat(view.missingCompanies()).extracting(Company::name).containsExactly("Meta");
        assertThat(view.competencies())
                .extracting(c -> c.competency().slug())
                .containsExactly("sql", "java", "ingles");
        TrackCompetencyView sql = view.competencies().get(0);
        assertThat(sql.competency().name()).isEqualTo("SQL e modelagem de dados");
        assertThat(sql.askedBy()).containsExactly("nubank", "google");
        assertThat(sql.jobs()).isEqualTo(11);
        assertThat(sql.requirement()).isEqualTo(Requirement.OBRIGATORIA);
        assertThat(sql.weight()).isEqualTo(0.425);
    }

    @Test
    void usesTheAreaOfTheChosenStack() {
        profiles.save(new CareerProfile(user, List.of("nubank", "google"), "react", Level.PLENO));

        LearningTrackView view = getTrack.execute(user);

        assertThat(view.missingCompanies()).extracting(Company::slug).containsExactly("nubank");
        assertThat(view.competencies())
                .extracting(c -> c.competency().slug())
                .containsExactly("java");
        assertThat(view.jobsAnalyzed()).isEqualTo(3);
    }

    @Test
    void isEmptyWhenNoChosenCompanyHasATrack() {
        profiles.save(new CareerProfile(user, List.of("meta"), "java-spring", Level.SENIOR));

        LearningTrackView view = getTrack.execute(user);

        assertThat(view.competencies()).isEmpty();
        assertThat(view.missingCompanies()).extracting(Company::slug).containsExactly("meta");
        assertThat(view.jobsAnalyzed()).isZero();
    }

    @Test
    void skipsACompanyThatNoLongerExists() {
        profiles.save(
                new CareerProfile(user, List.of("google", "nubank"), "java-spring", Level.JUNIOR));
        companies.remove("google");

        LearningTrackView view = getTrack.execute(user);

        assertThat(view.missingCompanies()).isEmpty();
        assertThat(view.jobsAnalyzed()).isEqualTo(17);
        assertThat(view.competencies().get(0).askedBy()).containsExactly("nubank");
    }

    @Test
    void failsWhenTheStackNoLongerExists() {
        profiles.save(new CareerProfile(user, List.of("nubank"), "java-spring", Level.JUNIOR));
        stacks.remove("java-spring");

        assertThatThrownBy(() -> getTrack.execute(user))
                .isInstanceOf(UnknownStackException.class)
                .hasMessageContaining("java-spring");
    }

    @Test
    void picksTheTrackOfTheSeniorityOfTheProfile() {
        tracks.save(track("nubank", "backend", null, Level.JUNIOR, 5, unit("java", 0.5, D, 5)));
        profiles.save(new CareerProfile(user, List.of("nubank"), "java-spring", Level.JUNIOR));
        assertThat(getTrack.execute(user).competencies())
                .extracting(c -> c.competency().slug())
                .containsExactly("java");

        profiles.save(new CareerProfile(user, List.of("nubank"), "java-spring", Level.PLENO));
        assertThat(getTrack.execute(user).competencies())
                .extracting(c -> c.competency().slug())
                .containsExactly("sql", "ingles");
    }

    @Test
    void findIsEmptyWithoutAProfile() {
        assertThat(assembler.find(user)).isEmpty();

        profiles.save(new CareerProfile(user, List.of("nubank"), "java-spring", Level.JUNIOR));

        assertThat(assembler.find(user)).isPresent();
    }

    @Test
    void statusComesFromTheActiveQuestionsAndChallengesOfTheStackArea() {
        Area backend = areas.save(CatalogFixtures.area("backend"));
        Area frontend = areas.save(CatalogFixtures.area("frontend"));
        questions.save(
                CatalogFixtures.question(
                        "q-sql-1", "Junção", backend, Difficulty.EASY, true, "sql"));
        questions.save(
                CatalogFixtures.question(
                        "q-sql-2", "Índice", backend, Difficulty.MEDIUM, true, "sql"));
        questions.save(
                CatalogFixtures.question(
                        "q-java", "Exceções", backend, Difficulty.EASY, true, "java"));
        questions.save(
                CatalogFixtures.question(
                        "q-java-front", "Outra área", frontend, Difficulty.EASY, true, "java"));
        questions.save(
                CatalogFixtures.question(
                        "q-java-off", "Inativa", backend, Difficulty.EASY, false, "java"));
        challenges.save(
                CatalogFixtures.challenge("c-java", "Desafio", backend, Difficulty.EASY, "java"));
        challenges.save(
                CatalogFixtures.challenge(
                        "c-java-front", "Outra área", frontend, Difficulty.EASY, "java"));
        profiles.save(
                new CareerProfile(user, List.of("nubank", "google"), "java-spring", Level.JUNIOR));
        progress.record(user, "q-sql-1", QuestionProgress.SOLVED);
        progress.record(user, "q-sql-2", QuestionProgress.SOLVED);
        progress.record(user, "q-java", QuestionProgress.SOLVED);
        progress.record(user, "q-java-front", QuestionProgress.SOLVED);
        progress.record(user, "q-java-off", QuestionProgress.SOLVED);
        progress.record(user, "c-java-front", QuestionProgress.SOLVED);

        LearningTrackView view = getTrack.execute(user);

        assertThat(statusOf(view, "sql")).isEqualTo(CompetencyStatus.DONE);
        assertThat(statusOf(view, "java")).isEqualTo(CompetencyStatus.IN_PROGRESS);
        assertThat(view.progressOf("c-java")).isEqualTo(QuestionProgress.NOT_STARTED);

        progress.record(user, "c-java", QuestionProgress.SOLVED);
        assertThat(statusOf(getTrack.execute(user), "java")).isEqualTo(CompetencyStatus.DONE);

        questions.save(
                CatalogFixtures.question(
                        "q-sql-3", "Transação", backend, Difficulty.HARD, true, "sql"));
        assertThat(statusOf(getTrack.execute(user), "sql")).isEqualTo(CompetencyStatus.IN_PROGRESS);
    }

    @Test
    void aChallengeAloneStartsTheCompetency() {
        Area backend = areas.save(CatalogFixtures.area("backend"));
        questions.save(
                CatalogFixtures.question(
                        "q-java", "Exceções", backend, Difficulty.EASY, true, "java"));
        challenges.save(
                CatalogFixtures.challenge("c-java", "Desafio", backend, Difficulty.EASY, "java"));
        profiles.save(
                new CareerProfile(user, List.of("nubank", "google"), "java-spring", Level.JUNIOR));
        progress.record(user, "c-java", QuestionProgress.ATTEMPTED);

        assertThat(statusOf(getTrack.execute(user), "java"))
                .isEqualTo(CompetencyStatus.IN_PROGRESS);
    }

    @Test
    void aCompetencyWithoutQuestionsStaysNotStarted() {
        areas.save(CatalogFixtures.area("backend"));
        profiles.save(new CareerProfile(user, List.of("nubank"), "java-spring", Level.JUNIOR));

        LearningTrackView view = getTrack.execute(user);

        assertThat(view.competencies())
                .extracting(TrackCompetencyView::status)
                .containsOnly(CompetencyStatus.NOT_STARTED);
    }

    private static CompetencyStatus statusOf(LearningTrackView view, String slug) {
        return view.competencies().stream()
                .filter(item -> item.competency().slug().equals(slug))
                .findFirst()
                .orElseThrow()
                .status();
    }
}
