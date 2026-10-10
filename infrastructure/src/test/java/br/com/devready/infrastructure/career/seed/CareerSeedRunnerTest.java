package br.com.devready.infrastructure.career.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.dto.CareerSeed;
import br.com.devready.application.career.dto.CareerSeedReport;
import br.com.devready.application.career.dto.CompanySeed;
import br.com.devready.application.career.dto.CompetencySeed;
import br.com.devready.application.career.dto.MaterialSeed;
import br.com.devready.application.career.dto.OnboardingOptions;
import br.com.devready.application.career.dto.StackSeed;
import br.com.devready.application.career.dto.TrackSeed;
import br.com.devready.application.career.dto.TrackUnitSeed;
import br.com.devready.application.career.port.CareerSeedSource;
import br.com.devready.application.career.port.CompanyRepository;
import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.application.career.port.StackRepository;
import br.com.devready.application.career.port.TrackRepository;
import br.com.devready.application.career.usecase.ListOnboardingOptionsUseCase;
import br.com.devready.application.career.usecase.SeedCareerUseCase;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.TagRepository;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.career.Requirement;
import br.com.devready.domain.career.Track;
import br.com.devready.domain.career.TrackUnit;
import br.com.devready.infrastructure.InfrastructureTestApplication;
import br.com.devready.infrastructure.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
        classes = InfrastructureTestApplication.class,
        properties = {"devready.seed.enabled=true", "devready.seed.location=classpath:seed"})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CareerSeedRunnerTest {

    @Autowired private ListOnboardingOptionsUseCase listOnboardingOptions;
    @Autowired private SeedCareerUseCase seedCareer;
    @Autowired private CareerSeedSource source;
    @Autowired private CompanyRepository companies;
    @Autowired private StackRepository stacks;
    @Autowired private AreaRepository areas;
    @Autowired private TagRepository tags;
    @Autowired private CompetencyRepository competencies;
    @Autowired private TrackRepository tracks;
    @Autowired private IdGenerator ids;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void seedRanAfterTheCatalogAndRunningAgainChangesNothing() {
        OnboardingOptions options = listOnboardingOptions.execute();

        assertThat(options.companies()).hasSize(10);
        assertThat(options.companies().subList(0, 5))
                .extracting(Company::group)
                .containsOnly(CompanyGroup.BIG_TECH);
        assertThat(options.stacks()).hasSize(8);

        assertThat(seedCareer.execute().total()).isZero();
        CareerSeed files = source.load();
        assertThat(competencies.findAll())
                .extracting(Competency::slug)
                .containsExactlyInAnyOrderElementsOf(
                        files.competencies().stream().map(CompetencySeed::slug).toList());
        assertThat(tracks.findAll()).hasSize(files.tracks().size());
    }

    @Test
    void databaseFailureHalfwayThroughTheWritesUndoesWhatWasAlreadyWritten() {
        CareerSeed seed =
                new CareerSeed(
                        List.of(
                                new CompanySeed(
                                        "zz-desfeita", "Desfeita", "tech-brasil", "", List.of())),
                        List.of(new StackSeed("zz-stack", "N".repeat(81), "backend")),
                        List.of(),
                        List.of(),
                        List.of());
        CareerSeedRunner runner =
                new CareerSeedRunner(
                        new SeedCareerUseCase(
                                () -> seed,
                                companies,
                                stacks,
                                areas,
                                tags,
                                competencies,
                                tracks,
                                ids),
                        transactionManager);

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(companies.findBySlug("zz-desfeita")).isEmpty();
        assertThat(stacks.findBySlug("zz-stack")).isEmpty();
    }

    @Test
    void databaseFailureWhileWritingACompetencyUndoesTheCompanyAlreadyWritten() {
        int tracksBefore = tracks.findAll().size();
        CareerSeed seed =
                new CareerSeed(
                        List.of(
                                new CompanySeed(
                                        "zz-desfeita-2", "Desfeita", "tech-brasil", "", List.of())),
                        List.of(),
                        List.of(
                                new CompetencySeed(
                                        "zz-competencia",
                                        "Competência",
                                        "fundamento",
                                        "Resumo.",
                                        "easy",
                                        List.of())),
                        List.of(
                                new MaterialSeed(
                                        "zz-competencia",
                                        "T".repeat(201),
                                        "https://exemplo.com",
                                        "livro",
                                        "pt",
                                        true)),
                        List.of());
        CareerSeedRunner runner =
                new CareerSeedRunner(
                        new SeedCareerUseCase(
                                () -> seed,
                                companies,
                                stacks,
                                areas,
                                tags,
                                competencies,
                                tracks,
                                ids),
                        transactionManager);

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(companies.findBySlug("zz-desfeita-2")).isEmpty();
        assertThat(competencies.findAll())
                .extracting(Competency::slug)
                .doesNotContain("zz-competencia");
        assertThat(tracks.findAll()).hasSize(tracksBefore);
    }

    @Test
    void theSameSeedRunTwiceAgainstTheDatabaseWritesNothingTheSecondTime() {
        CareerSeed seed = fullSeed();
        try {
            CareerSeedReport first = run(seed);
            assertThat(first.insertedCompetencies() + first.updatedCompetencies()).isEqualTo(3);
            assertThat(first.insertedTracks() + first.updatedTracks()).isEqualTo(2);
            List<String> rowsAfterFirst = rowVersions();
            assertThat(rowsAfterFirst).hasSize(20);

            CareerSeedReport second = run(seed);

            assertThat(second.total()).isZero();
            assertThat(rowVersions()).isEqualTo(rowsAfterFirst);
        } finally {
            restoreRealSeed();
        }
    }

    @Test
    void removingACompetencyAndATrackThatUsesItInTheSameSeedRespectsTheForeignKeys() {
        try {
            run(fullSeed());
            Competency git = competency("zz-git");
            Track google = track("google");
            Track nubank = track("nubank");
            assertThat(count("materials", "competency_id", git.id())).isEqualTo(1);
            assertThat(count("competency_tags", "competency_id", git.id())).isEqualTo(1);
            assertThat(count("track_units", "track_id", google.id())).isEqualTo(2);
            CareerSeed smaller =
                    new CareerSeed(
                            List.of(),
                            List.of(),
                            List.of(sqlSeed(), javaSeed()),
                            sqlMaterials(),
                            List.of(
                                    new TrackSeed(
                                            "nubank",
                                            "backend",
                                            "todas",
                                            "todas",
                                            17,
                                            List.of(
                                                    new TrackUnitSeed(
                                                            "zz-java", 0.0588, "desejavel", 1),
                                                    new TrackUnitSeed(
                                                            "zz-sql", 0.4118, "obrigatoria", 7)))));

            CareerSeedReport report = run(smaller);

            assertThat(report.removedTracks()).isEqualTo(1);
            assertThat(report.removedCompetencies()).isEqualTo(1);
            assertThat(report.updatedTracks()).isEqualTo(1);
            assertThat(report.updatedCompetencies()).isEqualTo(1);
            assertThat(tracks.findAll())
                    .containsExactly(
                            new Track(
                                    nubank.id(),
                                    "nubank",
                                    "backend",
                                    null,
                                    null,
                                    17,
                                    List.of(
                                            new TrackUnit(
                                                    "zz-java", 0.0588, Requirement.DESEJAVEL, 1),
                                            new TrackUnit(
                                                    "zz-sql",
                                                    0.4118,
                                                    Requirement.OBRIGATORIA,
                                                    7))));
            assertThat(competencies.findAll())
                    .extracting(Competency::slug)
                    .containsExactlyInAnyOrder("zz-sql", "zz-java");
            assertThat(count("materials", "competency_id", git.id())).isZero();
            assertThat(count("competency_tags", "competency_id", git.id())).isZero();
            assertThat(count("track_units", "track_id", google.id())).isZero();
            assertThat(competency("zz-java").materials()).isEmpty();
        } finally {
            restoreRealSeed();
        }
    }

    private static CareerSeed fullSeed() {
        List<MaterialSeed> materials = new ArrayList<>(sqlMaterials());
        materials.add(
                new MaterialSeed(
                        "zz-java",
                        "Documentação do Java",
                        "https://docs.oracle.com/en/java/",
                        "documentacao",
                        "en",
                        true));
        materials.add(
                new MaterialSeed(
                        "zz-git",
                        "Pro Git",
                        "https://git-scm.com/book/pt-br/v2",
                        "livro",
                        "pt",
                        true));
        return new CareerSeed(
                List.of(),
                List.of(),
                List.of(
                        sqlSeed(),
                        javaSeed(),
                        new CompetencySeed(
                                "zz-git",
                                "Git e versionamento",
                                "pratica",
                                "",
                                "medium",
                                List.of("git"))),
                materials,
                List.of(
                        new TrackSeed(
                                "nubank",
                                "backend",
                                "todas",
                                "todas",
                                17,
                                List.of(
                                        new TrackUnitSeed("zz-sql", 0.4118, "obrigatoria", 7),
                                        new TrackUnitSeed("zz-java", 0.0588, "desejavel", 1),
                                        new TrackUnitSeed("zz-git", 0.59, "obrigatoria", 10))),
                        new TrackSeed(
                                "google",
                                "backend",
                                "java-spring",
                                "junior",
                                5,
                                List.of(
                                        new TrackUnitSeed("zz-git", 0.8, "obrigatoria", 4),
                                        new TrackUnitSeed("zz-sql", 0.4, "desejavel", 2)))));
    }

    private static CompetencySeed sqlSeed() {
        return new CompetencySeed(
                "zz-sql",
                "SQL e modelagem de dados",
                "fundamento",
                "Consultas, índices e modelagem.\n",
                "medium",
                List.of("sql", "orm", "banco-de-dados"));
    }

    private static CompetencySeed javaSeed() {
        return new CompetencySeed("zz-java", "Java", "linguagem", "", "easy", List.of("java"));
    }

    private static List<MaterialSeed> sqlMaterials() {
        return List.of(
                new MaterialSeed(
                        "zz-sql",
                        "Use The Index, Luke",
                        "https://use-the-index-luke.com/",
                        "livro",
                        "en",
                        true),
                new MaterialSeed(
                        "zz-sql",
                        "Documentação do PostgreSQL",
                        "https://www.postgresql.org/docs/",
                        "documentacao",
                        "en",
                        true),
                new MaterialSeed(
                        "zz-sql",
                        "Artigo sobre índices",
                        "https://exemplo.com/indices",
                        "artigo",
                        "pt",
                        false));
    }

    private CareerSeedReport run(CareerSeed seed) {
        SeedCareerUseCase useCase =
                new SeedCareerUseCase(
                        () -> seed, companies, stacks, areas, tags, competencies, tracks, ids);
        return new TransactionTemplate(transactionManager).execute(status -> useCase.execute());
    }

    private void restoreRealSeed() {
        new TransactionTemplate(transactionManager).execute(status -> seedCareer.execute());
    }

    private List<String> rowVersions() {
        return jdbc.queryForList(
                "select 'competencies ' || id || ' ' || xmin::text from competencies"
                        + " union all select 'competency_tags ' || competency_id || ' ' || tag_id"
                        + " || ' ' || xmin::text from competency_tags"
                        + " union all select 'materials ' || competency_id || ' ' || position"
                        + " || ' ' || xmin::text from materials"
                        + " union all select 'tracks ' || id || ' ' || xmin::text from tracks"
                        + " union all select 'track_units ' || track_id || ' ' || position"
                        + " || ' ' || xmin::text from track_units"
                        + " order by 1",
                String.class);
    }

    private Competency competency(String slug) {
        return competencies.findAll().stream()
                .filter(c -> c.slug().equals(slug))
                .findFirst()
                .orElseThrow();
    }

    private Track track(String company) {
        return tracks.findByCompanies(List.of(company)).stream().findFirst().orElseThrow();
    }

    private int count(String table, String column, UUID id) {
        return jdbc.queryForObject(
                "select count(*) from " + table + " where " + column + " = ?", Integer.class, id);
    }
}
