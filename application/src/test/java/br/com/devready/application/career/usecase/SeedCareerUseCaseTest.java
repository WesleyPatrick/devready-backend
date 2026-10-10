package br.com.devready.application.career.usecase;

import static br.com.devready.application.career.fake.CareerSeedFixtures.company;
import static br.com.devready.application.career.fake.CareerSeedFixtures.competency;
import static br.com.devready.application.career.fake.CareerSeedFixtures.material;
import static br.com.devready.application.career.fake.CareerSeedFixtures.track;
import static br.com.devready.application.career.fake.CareerSeedFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.dto.CareerSeed;
import br.com.devready.application.career.dto.CompanySeed;
import br.com.devready.application.career.dto.CompetencySeed;
import br.com.devready.application.career.dto.MaterialSeed;
import br.com.devready.application.career.dto.StackSeed;
import br.com.devready.application.career.dto.TrackSeed;
import br.com.devready.application.career.dto.TrackUnitSeed;
import br.com.devready.application.career.exception.InvalidCareerSeedException;
import br.com.devready.application.career.fake.CareerSeedFixtures;
import br.com.devready.application.career.fake.InMemoryCompanyRepository;
import br.com.devready.application.career.fake.InMemoryCompetencyRepository;
import br.com.devready.application.career.fake.InMemoryStackRepository;
import br.com.devready.application.career.fake.InMemoryTrackRepository;
import br.com.devready.application.catalog.fake.InMemoryAreaRepository;
import br.com.devready.application.catalog.fake.InMemoryTagRepository;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.career.CompetencyCategory;
import br.com.devready.domain.career.Material;
import br.com.devready.domain.career.Requirement;
import br.com.devready.domain.career.Track;
import br.com.devready.domain.career.TrackUnit;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.catalog.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeedCareerUseCaseTest {

    private final InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
    private final InMemoryStackRepository stacks = new InMemoryStackRepository();
    private final InMemoryAreaRepository areas = new InMemoryAreaRepository();
    private final InMemoryCompetencyRepository competencies = new InMemoryCompetencyRepository();
    private final InMemoryTrackRepository tracks = new InMemoryTrackRepository();
    private final InMemoryTagRepository tags = new InMemoryTagRepository();
    private final AtomicReference<CareerSeed> seed = new AtomicReference<>();

    private SeedCareerUseCase useCase() {
        return new SeedCareerUseCase(
                seed::get, companies, stacks, areas, tags, competencies, tracks, UUID::randomUUID);
    }

    @BeforeEach
    void catalog() {
        for (String area : List.of("backend", "frontend")) {
            areas.save(
                    new Area(
                            UUID.randomUUID(), area, area, new SessionConfig(8, 2, 45, 0.5), true));
        }
        for (String tag : List.of("sql", "java")) {
            tags.save(new Tag(UUID.randomUUID(), tag, tag));
        }
    }

    @Test
    void insertsThenUpdatesBySlugWithoutDuplicating() {
        seed.set(
                new CareerSeed(
                        List.of(new CompanySeed("nubank", "Nubank", "tech-brasil", "", List.of())),
                        List.of(new StackSeed("java-spring", "Java e Spring", "backend")),
                        List.of(),
                        List.of(),
                        List.of()));
        var first = useCase().execute();
        assertThat(first.insertedCompanies()).isEqualTo(1);
        assertThat(first.insertedStacks()).isEqualTo(1);

        seed.set(
                new CareerSeed(
                        List.of(
                                new CompanySeed(
                                        "nubank",
                                        "Nubank",
                                        "tech-brasil",
                                        "Entrevista em quatro etapas.",
                                        List.of("https://nu.com/careers"))),
                        List.of(new StackSeed("java-spring", "Java e Spring", "backend")),
                        List.of(),
                        List.of(),
                        List.of()));
        var second = useCase().execute();

        assertThat(second.updatedCompanies()).isEqualTo(1);
        assertThat(second.updatedStacks()).isZero();
        assertThat(second.total()).isEqualTo(1);
        assertThat(companies.count()).isEqualTo(1);
        var company = companies.findBySlug("nubank").orElseThrow();
        assertThat(company.about()).isEqualTo("Entrevista em quatro etapas.");
        assertThat(company.group()).isEqualTo(CompanyGroup.TECH_BRASIL);

        assertThat(useCase().execute().total()).isZero();
    }

    @Test
    void keepsTheIdWhenUpdating() {
        seed.set(
                new CareerSeed(
                        List.of(new CompanySeed("nubank", "Nubank", "tech-brasil", "", List.of())),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));
        useCase().execute();
        UUID id = companies.findBySlug("nubank").orElseThrow().id();
        seed.set(
                new CareerSeed(
                        List.of(new CompanySeed("nubank", "Nu", "tech-brasil", "", List.of())),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));
        useCase().execute();
        assertThat(companies.findBySlug("nubank").orElseThrow().id()).isEqualTo(id);
    }

    @Test
    void updatesAStackBySlugKeepingItsId() {
        seed.set(
                new CareerSeed(
                        List.of(),
                        List.of(new StackSeed("java-spring", "Java", "backend")),
                        List.of(),
                        List.of(),
                        List.of()));
        useCase().execute();
        UUID id = stacks.findBySlug("java-spring").orElseThrow().id();

        seed.set(
                new CareerSeed(
                        List.of(),
                        List.of(new StackSeed("java-spring", "Java e Spring", "backend")),
                        List.of(),
                        List.of(),
                        List.of()));
        var report = useCase().execute();

        assertThat(report.updatedStacks()).isEqualTo(1);
        assertThat(report.insertedStacks()).isZero();
        assertThat(stacks.count()).isEqualTo(1);
        var stack = stacks.findBySlug("java-spring").orElseThrow();
        assertThat(stack.name()).isEqualTo("Java e Spring");
        assertThat(stack.id()).isEqualTo(id);
    }

    @Test
    void keepsADeactivatedCompanyInactiveWhenNothingChanged() {
        companies.save(deactivatedNubank(""));
        seed.set(
                new CareerSeed(
                        List.of(new CompanySeed("nubank", "Nubank", "tech-brasil", "", List.of())),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));

        var report = useCase().execute();

        assertThat(report.total()).isZero();
        assertThat(companies.findBySlug("nubank").orElseThrow().active()).isFalse();
    }

    @Test
    void keepsADeactivatedCompanyInactiveWhenItIsUpdated() {
        Company before = deactivatedNubank("");
        companies.save(before);
        seed.set(
                new CareerSeed(
                        List.of(
                                new CompanySeed(
                                        "nubank",
                                        "Nubank",
                                        "tech-brasil",
                                        "Entrevista em quatro etapas.",
                                        List.of())),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));

        var report = useCase().execute();

        assertThat(report.updatedCompanies()).isEqualTo(1);
        var after = companies.findBySlug("nubank").orElseThrow();
        assertThat(after.about()).isEqualTo("Entrevista em quatro etapas.");
        assertThat(after.active()).isFalse();
        assertThat(after.id()).isEqualTo(before.id());
    }

    @Test
    void rejectsStackOfUnknownAreaWritingNothing() {
        seed.set(
                new CareerSeed(
                        List.of(new CompanySeed("nubank", "Nubank", "tech-brasil", "", List.of())),
                        List.of(
                                new StackSeed("java-spring", "Java e Spring", "backend"),
                                new StackSeed("swift", "Swift", "mobile")),
                        List.of(),
                        List.of(),
                        List.of()));

        assertThatThrownBy(() -> useCase().execute())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("swift")
                .hasMessageContaining("mobile");

        assertThat(companies.count()).isZero();
        assertThat(stacks.count()).isZero();
    }

    @Test
    void rejectsUnknownGroupWritingNothing() {
        seed.set(
                new CareerSeed(
                        List.of(
                                new CompanySeed("nubank", "Nubank", "tech-brasil", "", List.of()),
                                new CompanySeed("x", "X", "startup", "", List.of())),
                        List.of(new StackSeed("java-spring", "Java e Spring", "backend")),
                        List.of(),
                        List.of(),
                        List.of()));

        assertThatThrownBy(() -> useCase().execute())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("x")
                .hasMessageContaining("startup");

        assertThat(companies.count()).isZero();
        assertThat(stacks.count()).isZero();
    }

    @Test
    void rejectsStackWithInvalidSlugWritingNothing() {
        seed.set(
                new CareerSeed(
                        List.of(new CompanySeed("nubank", "Nubank", "tech-brasil", "", List.of())),
                        List.of(new StackSeed("Java Spring", "Java e Spring", "backend")),
                        List.of(),
                        List.of(),
                        List.of()));

        assertThatThrownBy(() -> useCase().execute())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("Java Spring");

        assertThat(companies.count()).isZero();
        assertThat(stacks.count()).isZero();
    }

    @Test
    void rejectsRepeatedSlugsInTheSameSeedWritingNothing() {
        seed.set(
                new CareerSeed(
                        List.of(
                                new CompanySeed("nubank", "Nubank", "tech-brasil", "", List.of()),
                                new CompanySeed("nubank", "Nu", "tech-brasil", "", List.of())),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));
        assertThatThrownBy(() -> useCase().execute())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("nubank");
        assertThat(companies.count()).isZero();

        seed.set(
                new CareerSeed(
                        List.of(),
                        List.of(
                                new StackSeed("java-spring", "Java", "backend"),
                                new StackSeed("java-spring", "Java e Spring", "backend")),
                        List.of(),
                        List.of(),
                        List.of()));
        assertThatThrownBy(() -> useCase().execute())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("java-spring");
        assertThat(stacks.count()).isZero();
    }

    @Test
    void rejectsANullCareerPageNamingTheCompanyWritingNothing() {
        List<String> careers = new ArrayList<>();
        careers.add(null);
        seed.set(
                new CareerSeed(
                        List.of(new CompanySeed("nubank", "Nubank", "tech-brasil", "", careers)),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));

        assertThatThrownBy(() -> useCase().execute())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("nubank");

        assertThat(companies.count()).isZero();
    }

    @Test
    void treatsNullListsAsEmpty() {
        seed.set(new CareerSeed(null, null, List.of(), List.of(), List.of()));

        var report = useCase().execute();

        assertThat(report.total()).isZero();
        assertThat(companies.count()).isZero();
        assertThat(stacks.count()).isZero();
    }

    @Test
    void insertsCompetenciesWithMaterialsAndTracksThenChangesNothing() {
        seed.set(CareerSeedFixtures.seed());

        var first = useCase().execute();

        assertThat(first.insertedCompetencies()).isEqualTo(2);
        assertThat(first.insertedTracks()).isEqualTo(2);
        Competency sql = competencies.find("sql").orElseThrow();
        assertThat(sql.category()).isEqualTo(CompetencyCategory.FUNDAMENTO);
        assertThat(sql.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(sql.summary()).isEqualTo("Resumo de sql.");
        assertThat(sql.tagSlugs()).containsExactly("sql");
        assertThat(sql.materials())
                .extracting(Material::title)
                .containsExactly("Use The Index, Luke", "Tutorial do PostgreSQL");
        assertThat(competencies.find("java").orElseThrow().materials()).isEmpty();
        Track nubank = trackOf("nubank");
        assertThat(nubank.stackSlug()).isNull();
        assertThat(nubank.seniority()).isNull();
        assertThat(nubank.jobs()).isEqualTo(17);
        assertThat(nubank.units())
                .extracting(TrackUnit::competencySlug)
                .containsExactly("sql", "java");
        assertThat(nubank.units().get(1).requirement()).isEqualTo(Requirement.DESEJAVEL);
        Track google = trackOf("google");
        assertThat(google.stackSlug()).isEqualTo("java-spring");
        assertThat(google.seniority()).isEqualTo(Level.JUNIOR);

        assertThat(useCase().execute().total()).isZero();
    }

    @Test
    void updatesACompetencyAndATrackKeepingTheirIds() {
        seed.set(CareerSeedFixtures.seed());
        useCase().execute();
        UUID sqlId = competencies.find("sql").orElseThrow().id();
        UUID nubankId = trackOf("nubank").id();
        CareerSeed base = CareerSeedFixtures.seed();
        seed.set(
                new CareerSeed(
                        base.companies(),
                        base.stacks(),
                        List.of(
                                new CompetencySeed(
                                        "sql",
                                        "SQL e modelagem de dados",
                                        "fundamento",
                                        "Outro resumo.",
                                        "hard",
                                        List.of("sql")),
                                competency("java", "linguagem", "medium", "java")),
                        base.materials(),
                        List.of(
                                track(
                                        "nubank",
                                        "backend",
                                        "todas",
                                        "todas",
                                        17,
                                        unit("java", 0.29, "desejavel", 5),
                                        unit("sql", 0.5, "obrigatoria", 9)),
                                base.tracks().get(1))));

        var report = useCase().execute();

        assertThat(report.updatedCompetencies()).isEqualTo(1);
        assertThat(report.updatedTracks()).isEqualTo(1);
        assertThat(report.insertedCompetencies() + report.insertedTracks()).isZero();
        Competency sql = competencies.find("sql").orElseThrow();
        assertThat(sql.id()).isEqualTo(sqlId);
        assertThat(sql.difficulty()).isEqualTo(Difficulty.HARD);
        Track nubank = trackOf("nubank");
        assertThat(nubank.id()).isEqualTo(nubankId);
        assertThat(nubank.units())
                .extracting(TrackUnit::competencySlug)
                .containsExactly("java", "sql");
    }

    @Test
    void removesTracksAndCompetenciesThatLeftTheFilesButKeepsCompanies() {
        seed.set(CareerSeedFixtures.seed());
        useCase().execute();
        CareerSeed base = CareerSeedFixtures.seed();
        seed.set(
                new CareerSeed(
                        List.of(company("nubank")),
                        base.stacks(),
                        List.of(competency("sql", "fundamento", "medium", "sql")),
                        base.materials(),
                        List.of(
                                track(
                                        "nubank",
                                        "backend",
                                        "todas",
                                        "todas",
                                        17,
                                        unit("sql", 0.41, "obrigatoria", 7)))));

        var report = useCase().execute();

        assertThat(report.removedTracks()).isEqualTo(1);
        assertThat(report.removedCompetencies()).isEqualTo(1);
        assertThat(report.updatedTracks()).isEqualTo(1);
        assertThat(tracks.count()).isEqualTo(1);
        assertThat(competencies.find("java")).isEmpty();
        assertThat(companies.findBySlug("google")).isPresent();
    }

    @Test
    void rejectsBrokenCompetenciesAndMaterialsWritingNothing() {
        CareerSeed base = CareerSeedFixtures.seed();
        List<TrackSeed> noTracks = List.of();
        assertRejectedWritingNothing(
                new CareerSeed(
                        base.companies(),
                        base.stacks(),
                        List.of(
                                competency("sql", "fundamento", "medium"),
                                competency("sql", "fundamento", "easy")),
                        List.of(),
                        noTracks),
                "sql",
                "repetida");
        assertRejectedWritingNothing(
                new CareerSeed(
                        base.companies(),
                        base.stacks(),
                        List.of(competency("sql", "fundamento", "medium", "cobol")),
                        List.of(),
                        noTracks),
                "sql",
                "cobol");
        assertRejectedWritingNothing(
                new CareerSeed(
                        base.companies(),
                        base.stacks(),
                        List.of(competency("sql", "dados", "medium")),
                        List.of(),
                        noTracks),
                "sql",
                "dados");
        assertRejectedWritingNothing(
                new CareerSeed(
                        base.companies(),
                        base.stacks(),
                        List.of(competency("sql", "fundamento", "dificil")),
                        List.of(),
                        noTracks),
                "sql",
                "dificil");
        assertRejectedWritingNothing(
                new CareerSeed(
                        base.companies(),
                        base.stacks(),
                        List.of(competency("sql", "fundamento", "medium")),
                        List.of(material("rust", "Livro de Rust")),
                        noTracks),
                "Livro de Rust",
                "rust");
        assertRejectedWritingNothing(
                new CareerSeed(
                        base.companies(),
                        base.stacks(),
                        List.of(competency("sql", "fundamento", "medium")),
                        List.of(
                                new MaterialSeed(
                                        "sql", "Podcast", "https://x", "podcast", "pt", true)),
                        noTracks),
                "Podcast",
                "podcast");
    }

    @Test
    void rejectsBrokenTracksWritingNothing() {
        CareerSeed base = CareerSeedFixtures.seed();
        TrackUnitSeed sql = unit("sql", 0.5, "obrigatoria", 5);
        List<Map.Entry<TrackSeed, List<String>>> cases =
                List.of(
                        Map.entry(
                                track("xpto", "backend", "todas", "todas", 10, sql),
                                List.of("xpto", "empresa desconhecida")),
                        Map.entry(
                                track("nubank", "mobile", "todas", "todas", 10, sql),
                                List.of("mobile", "área desconhecida")),
                        Map.entry(
                                track("nubank", "backend", "cobol", "todas", 10, sql),
                                List.of("nubank/backend/todas/cobol", "stack desconhecida")),
                        Map.entry(
                                track("nubank", "backend", "react", "todas", 10, sql),
                                List.of("react", "frontend")),
                        Map.entry(
                                track("nubank", "backend", null, "todas", 10, sql),
                                List.of("sem stack")),
                        Map.entry(
                                track("nubank", "backend", "todas", "estagio", 10, sql),
                                List.of("nubank/backend/estagio/todas", "estagio")),
                        Map.entry(
                                track(
                                        "nubank",
                                        "backend",
                                        "todas",
                                        "todas",
                                        10,
                                        unit("kotlin", 0.5, "obrigatoria", 5)),
                                List.of("kotlin", "competência desconhecida")),
                        Map.entry(
                                track(
                                        "nubank",
                                        "backend",
                                        "todas",
                                        "todas",
                                        10,
                                        unit("sql", 1.4, "obrigatoria", 5)),
                                List.of("sql", "Peso fora de 0 a 1")),
                        Map.entry(
                                track("nubank", "backend", "todas", "todas", 10, sql, sql),
                                List.of("sql", "repetida")),
                        Map.entry(
                                track(
                                        "nubank",
                                        "backend",
                                        "todas",
                                        "todas",
                                        10,
                                        unit("sql", 0.5, "talvez", 5)),
                                List.of("talvez")),
                        Map.entry(
                                track("nubank", "backend", "java-spring", "junior", 4, sql),
                                List.of("nubank/backend/junior/java-spring", "mais vagas")),
                        Map.entry(
                                track(
                                        "nubank",
                                        "backend",
                                        "todas",
                                        "todas",
                                        10,
                                        new TrackUnitSeed("sql", null, "obrigatoria", 5)),
                                List.of("sem peso")),
                        Map.entry(
                                new TrackSeed(
                                        "nubank", "backend", "todas", "todas", null, List.of(sql)),
                                List.of("número de vagas")),
                        Map.entry(
                                track("nubank", "backend", "todas", "todas", 10),
                                List.of("sem competências")));
        for (var broken : cases) {
            assertRejectedWritingNothing(
                    new CareerSeed(
                            base.companies(),
                            base.stacks(),
                            base.competencies(),
                            List.of(),
                            List.of(broken.getKey())),
                    broken.getValue().toArray(String[]::new));
        }

        TrackSeed area = track("nubank", "backend", "todas", "todas", 10, sql);
        assertRejectedWritingNothing(
                new CareerSeed(
                        base.companies(),
                        base.stacks(),
                        base.competencies(),
                        List.of(),
                        List.of(area, area)),
                "nubank/backend/todas/todas",
                "repetida");
    }

    @Test
    void rejectsDroppingACompetencyThatATrackStillUsesKeepingTheDatabase() {
        seed.set(CareerSeedFixtures.seed());
        useCase().execute();
        CareerSeed base = CareerSeedFixtures.seed();
        seed.set(
                new CareerSeed(
                        base.companies(),
                        base.stacks(),
                        List.of(competency("sql", "fundamento", "medium", "sql")),
                        base.materials(),
                        base.tracks()));

        assertThatThrownBy(() -> useCase().execute())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("java")
                .hasMessageContaining("competência desconhecida");

        assertThat(competencies.count()).isEqualTo(2);
        assertThat(tracks.count()).isEqualTo(2);
    }

    private void assertRejectedWritingNothing(CareerSeed broken, String... fragments) {
        seed.set(broken);
        var thrown =
                assertThatThrownBy(() -> useCase().execute())
                        .isInstanceOf(InvalidCareerSeedException.class);
        for (String fragment : fragments) {
            thrown.hasMessageContaining(fragment);
        }
        assertThat(companies.count()).isZero();
        assertThat(stacks.count()).isZero();
        assertThat(competencies.count()).isZero();
        assertThat(tracks.count()).isZero();
    }

    private Track trackOf(String company) {
        return tracks.findAll().stream()
                .filter(t -> t.companySlug().equals(company))
                .findFirst()
                .orElseThrow();
    }

    private static Company deactivatedNubank(String about) {
        return new Company(
                UUID.randomUUID(),
                "nubank",
                "Nubank",
                CompanyGroup.TECH_BRASIL,
                about,
                List.of(),
                false);
    }
}
