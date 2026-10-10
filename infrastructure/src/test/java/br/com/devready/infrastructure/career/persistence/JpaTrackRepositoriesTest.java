package br.com.devready.infrastructure.career.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.devready.application.catalog.dto.ChallengeSummary;
import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.application.catalog.dto.QuestionSummary;
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
import br.com.devready.domain.catalog.Tag;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.catalog.persistence.JpaAreaRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaChallengeRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaPracticeCatalog;
import br.com.devready.infrastructure.catalog.persistence.JpaQuestionRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaTagRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    PostgresTestcontainersConfiguration.class,
    JpaAreaRepository.class,
    JpaTagRepository.class,
    JpaQuestionRepository.class,
    JpaChallengeRepository.class,
    JpaPracticeCatalog.class,
    JpaCompanyRepository.class,
    JpaStackRepository.class,
    JpaCompetencyRepository.class,
    JpaTrackRepository.class
})
class JpaTrackRepositoriesTest {

    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaTagRepository tags;
    @Autowired private JpaQuestionRepository questions;
    @Autowired private JpaChallengeRepository challenges;
    @Autowired private JpaPracticeCatalog practice;
    @Autowired private JpaCompanyRepository companies;
    @Autowired private JpaStackRepository stacks;
    @Autowired private JpaCompetencyRepository competencies;
    @Autowired private JpaTrackRepository tracks;
    @Autowired private TestEntityManager entityManager;

    private static final Set<Level> ALL_LEVELS = Level.upTo(Level.SENIOR);

    @Test
    void savesACompetencyWithTagsAndMaterialsInOrderAndUpdatesItInPlace() {
        String tag = unique("t");
        tags.save(new Tag(UUID.randomUUID(), tag, "Tag"));
        Competency sql =
                competency(
                        unique("zz-sql"), Set.of(tag), List.of(material("um"), material("dois")));
        competencies.save(sql);
        entityManager.clear();

        assertThat(find(sql.slug())).isEqualTo(sql);

        Competency edited =
                new Competency(
                        sql.id(),
                        sql.slug(),
                        "SQL",
                        CompetencyCategory.FUNDAMENTO,
                        "Outro resumo.",
                        Difficulty.HARD,
                        Set.of(),
                        List.of(material("dois"), material("tres"), material("um")));
        competencies.save(edited);
        entityManager.clear();

        assertThat(find(sql.slug())).isEqualTo(edited);
    }

    @Test
    void deletingACompetencyTakesItsMaterialsAlong() {
        Competency competency = competency(unique("zz-del"), Set.of(), List.of(material("um")));
        competencies.save(competency);

        competencies.delete(competency.slug());
        entityManager.clear();

        assertThat(find(competency.slug())).isNull();
        Number rows =
                (Number)
                        entityManager
                                .getEntityManager()
                                .createNativeQuery(
                                        "select count(*) from materials where competency_id = :id")
                                .setParameter("id", competency.id())
                                .getSingleResult();
        assertThat(rows.intValue()).isZero();
    }

    @Test
    void savesTracksResolvingSlugsAndFindsThemByCompany() {
        Fixture f = fixture();
        Track area =
                new Track(
                        UUID.randomUUID(),
                        f.company(),
                        f.area(),
                        null,
                        null,
                        17,
                        List.of(
                                new TrackUnit(f.eda(), 0.53, Requirement.OBRIGATORIA, 9),
                                new TrackUnit(f.sql(), 0.41, Requirement.DESEJAVEL, 7)));
        Track junior =
                new Track(
                        UUID.randomUUID(),
                        f.company(),
                        f.area(),
                        f.stack(),
                        Level.JUNIOR,
                        8,
                        List.of(new TrackUnit(f.sql(), 1, Requirement.OBRIGATORIA, 8)));
        Track other =
                new Track(
                        UUID.randomUUID(),
                        f.other(),
                        f.area(),
                        null,
                        null,
                        3,
                        List.of(new TrackUnit(f.eda(), 0.67, Requirement.OBRIGATORIA, 2)));
        tracks.save(area);
        tracks.save(junior);
        tracks.save(other);
        entityManager.clear();

        assertThat(tracks.findByCompanies(List.of(f.company())))
                .containsExactlyInAnyOrder(area, junior);
        assertThat(tracks.findAll()).contains(area, junior, other);
        assertThat(tracks.findByCompanies(List.of())).isEmpty();
    }

    @Test
    void savingATrackAgainUpdatesItInPlace() {
        Fixture f = fixture();
        Track first =
                new Track(
                        UUID.randomUUID(),
                        f.company(),
                        f.area(),
                        null,
                        null,
                        10,
                        List.of(
                                new TrackUnit(f.eda(), 0.5, Requirement.OBRIGATORIA, 5),
                                new TrackUnit(f.sql(), 0.4, Requirement.DESEJAVEL, 4)));
        tracks.save(first);
        Track second =
                new Track(
                        first.id(),
                        f.company(),
                        f.area(),
                        null,
                        null,
                        12,
                        List.of(new TrackUnit(f.eda(), 0.6, Requirement.OBRIGATORIA, 7)));
        tracks.save(second);
        entityManager.clear();

        assertThat(tracks.findByCompanies(List.of(f.company()))).containsExactly(second);
    }

    @Test
    void weightsWithTheFourDecimalsOfTheResearchComeBackExactly() {
        Fixture f = fixture();
        Track track =
                new Track(
                        UUID.randomUUID(),
                        f.company(),
                        f.area(),
                        null,
                        null,
                        17,
                        List.of(
                                new TrackUnit(f.eda(), 0.4118, Requirement.OBRIGATORIA, 7),
                                new TrackUnit(f.sql(), 0.0588, Requirement.DESEJAVEL, 1)));
        tracks.save(track);
        entityManager.clear();

        assertThat(tracks.findByCompanies(List.of(f.company()))).containsExactly(track);
    }

    @Test
    void deletingATrackTakesItsUnitsAlong() {
        Fixture f = fixture();
        Track track =
                new Track(
                        UUID.randomUUID(),
                        f.company(),
                        f.area(),
                        null,
                        null,
                        4,
                        List.of(new TrackUnit(f.eda(), 0.5, Requirement.OBRIGATORIA, 2)));
        tracks.save(track);

        tracks.delete(track.id());
        entityManager.clear();

        assertThat(tracks.findByCompanies(List.of(f.company()))).isEmpty();
        Number rows =
                (Number)
                        entityManager
                                .getEntityManager()
                                .createNativeQuery(
                                        "select count(*) from track_units where track_id = :id")
                                .setParameter("id", track.id())
                                .getSingleResult();
        assertThat(rows.intValue()).isZero();
    }

    @Test
    void aSecondTrackWithTheSameCompanyAreaStackAndSeniorityIsRejectedEvenWithNulls() {
        Fixture f = fixture();
        tracks.save(
                new Track(
                        UUID.randomUUID(),
                        f.company(),
                        f.area(),
                        null,
                        null,
                        5,
                        List.of(new TrackUnit(f.eda(), 0.4, Requirement.OBRIGATORIA, 2))));
        Track twin =
                new Track(
                        UUID.randomUUID(),
                        f.company(),
                        f.area(),
                        null,
                        null,
                        6,
                        List.of(new TrackUnit(f.sql(), 0.5, Requirement.OBRIGATORIA, 3)));

        assertThatThrownBy(() -> tracks.save(twin))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void reorderingTheUnitsOfASavedTrackPassesTheCommitTimeUniqueness() {
        Fixture f = fixture();
        Track first =
                new Track(
                        UUID.randomUUID(),
                        f.company(),
                        f.area(),
                        null,
                        null,
                        10,
                        List.of(
                                new TrackUnit(f.eda(), 0.5, Requirement.OBRIGATORIA, 5),
                                new TrackUnit(f.sql(), 0.4, Requirement.DESEJAVEL, 4)));
        tracks.save(first);
        Track swapped =
                new Track(
                        first.id(),
                        f.company(),
                        f.area(),
                        null,
                        null,
                        10,
                        List.of(
                                new TrackUnit(f.sql(), 0.4, Requirement.DESEJAVEL, 4),
                                new TrackUnit(f.eda(), 0.5, Requirement.OBRIGATORIA, 5)));

        tracks.save(swapped);

        assertThat(tracks.findByCompanies(List.of(f.company()))).containsExactly(swapped);
    }

    @Test
    void practiceCatalogListsActiveItemsOfTheAreaWithAnyOfTheTags() {
        String tagA = unique("ta");
        String tagB = unique("tb");
        String other = unique("tc");
        for (String tag : List.of(tagA, tagB, other)) {
            tags.save(new Tag(UUID.randomUUID(), tag, tag));
        }
        Area area = area();
        Area elsewhere = area();
        Question both =
                questions.save(
                        question(area, "Título A", Difficulty.MEDIUM, true, tagA, tagB, other));
        Question second = questions.save(question(area, "Título B", Difficulty.EASY, true, tagB));
        questions.save(question(area, "Inativa", Difficulty.EASY, false, tagA));
        questions.save(question(elsewhere, "Outra área", Difficulty.EASY, true, tagA));
        questions.save(question(area, "Sem a tag", Difficulty.EASY, true, other));
        Challenge challenge = challenges.save(challenge(area, Difficulty.HARD, tagA, other));
        entityManager.clear();

        assertThat(practice.activeQuestions(area.id(), Set.of(tagA, tagB), ALL_LEVELS))
                .containsExactlyInAnyOrder(
                        new QuestionSummary(
                                both.slug(), "Título A", Difficulty.MEDIUM, Level.JUNIOR),
                        new QuestionSummary(
                                second.slug(), "Título B", Difficulty.EASY, Level.JUNIOR));
        List<ChallengeSummary> found =
                practice.activeChallenges(area.id(), Set.of(tagA, other), ALL_LEVELS);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).slug()).isEqualTo(challenge.slug());
        assertThat(found.get(0).difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(found.get(0).languages()).containsExactly(Language.JAVA, Language.PYTHON);
        assertThat(practice.activeQuestions(area.id(), Set.of(), ALL_LEVELS)).isEmpty();
        assertThat(practice.activeChallenges(area.id(), Set.of(tagB), ALL_LEVELS)).isEmpty();
    }

    @Test
    void practiceCatalogKeepsOnlyTheAskedLevels() {
        String tag = unique("tn");
        tags.save(new Tag(UUID.randomUUID(), tag, tag));
        Area area = area();
        Question junior = questions.save(question(area, "Júnior", Difficulty.EASY, true, tag));
        Question pleno =
                questions.save(
                        atLevel(
                                question(area, "Pleno", Difficulty.MEDIUM, true, tag),
                                Level.PLENO));
        Question senior =
                questions.save(
                        atLevel(
                                question(area, "Sênior", Difficulty.HARD, true, tag),
                                Level.SENIOR));
        Challenge challenge =
                challenges.save(atLevel(challenge(area, Difficulty.HARD, tag), Level.SENIOR));
        entityManager.clear();

        assertThat(practice.activeQuestions(area.id(), Set.of(tag), Level.upTo(Level.PLENO)))
                .containsExactlyInAnyOrder(
                        new QuestionSummary(junior.slug(), "Júnior", Difficulty.EASY, Level.JUNIOR),
                        new QuestionSummary(pleno.slug(), "Pleno", Difficulty.MEDIUM, Level.PLENO));
        assertThat(practice.activeChallenges(area.id(), Set.of(tag), Level.upTo(Level.PLENO)))
                .isEmpty();
        assertThat(practice.activeChallenges(area.id(), Set.of(tag), ALL_LEVELS))
                .extracting(ChallengeSummary::slug, ChallengeSummary::level)
                .containsExactly(tuple(challenge.slug(), Level.SENIOR));
        assertThat(practice.activeProblems(area.id(), Set.of(Level.SENIOR)))
                .extracting(ProblemSummary::slug, ProblemSummary::level)
                .containsExactlyInAnyOrder(
                        tuple(senior.slug(), Level.SENIOR), tuple(challenge.slug(), Level.SENIOR));
        assertThat(practice.activeProblems(area.id(), Set.of())).isEmpty();
        assertThat(practice.activeQuestions(area.id(), Set.of(tag), Set.of())).isEmpty();
    }

    private record Fixture(
            String area, String company, String other, String stack, String eda, String sql) {}

    private Fixture fixture() {
        String area = area().slug();
        String company = unique("zz-empresa");
        String other = unique("zz-outra");
        companies.save(
                new Company(
                        UUID.randomUUID(),
                        company,
                        "Empresa",
                        CompanyGroup.TECH_BRASIL,
                        "",
                        List.of(),
                        true));
        companies.save(
                new Company(
                        UUID.randomUUID(),
                        other,
                        "Outra",
                        CompanyGroup.BIG_TECH,
                        "",
                        List.of(),
                        true));
        String stack = unique("zz-stack");
        stacks.save(new Stack(UUID.randomUUID(), stack, "Stack", area));
        String eda = unique("zz-eda");
        String sql = unique("zz-sql");
        competencies.save(competency(eda, Set.of(), List.of()));
        competencies.save(competency(sql, Set.of(), List.of()));
        return new Fixture(area, company, other, stack, eda, sql);
    }

    private Area area() {
        return areas.save(
                new Area(
                        UUID.randomUUID(),
                        unique("zz-area"),
                        "Área",
                        new SessionConfig(8, 2, 45, 0.5),
                        true));
    }

    private Competency find(String slug) {
        return competencies.findAll().stream()
                .filter(c -> c.slug().equals(slug))
                .findFirst()
                .orElse(null);
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Competency competency(
            String slug, Set<String> tagSlugs, List<Material> materials) {
        return new Competency(
                UUID.randomUUID(),
                slug,
                "Competência " + slug,
                CompetencyCategory.FUNDAMENTO,
                "Resumo.",
                Difficulty.MEDIUM,
                tagSlugs,
                materials);
    }

    private static Material material(String title) {
        return new Material(title, "https://exemplo.com/" + title, MaterialType.LIVRO, "pt", true);
    }

    private static Question question(
            Area area, String title, Difficulty difficulty, boolean active, String... tags) {
        return new Question(
                UUID.randomUUID(),
                unique("q"),
                title,
                area.id(),
                Level.JUNIOR,
                difficulty,
                "Enunciado",
                "Explicação",
                List.of(
                        new QuestionOption(UUID.randomUUID(), "A", true, 0),
                        new QuestionOption(UUID.randomUUID(), "B", false, 1)),
                Set.of(tags),
                active);
    }

    private static Challenge challenge(Area area, Difficulty difficulty, String... tags) {
        return new Challenge(
                UUID.randomUUID(),
                unique("c"),
                "Desafio",
                area.id(),
                Level.JUNIOR,
                difficulty,
                "Enunciado",
                "Abordagem",
                new ExecutionLimits(5000, 262144),
                ComparisonMode.EXACT,
                5,
                Map.of(
                        Language.PYTHON,
                        new ChallengeLanguage(Language.PYTHON, "f", "class Solution: pass", "ref"),
                        Language.JAVA,
                        new ChallengeLanguage(Language.JAVA, "f", "class Solution {}", "ref")),
                List.of(
                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                Set.of(tags),
                true);
    }

    private static Question atLevel(Question q, Level level) {
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

    private static Challenge atLevel(Challenge c, Level level) {
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
