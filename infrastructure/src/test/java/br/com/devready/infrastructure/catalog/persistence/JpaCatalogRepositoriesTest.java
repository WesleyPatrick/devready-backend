package br.com.devready.infrastructure.catalog.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    PostgresTestcontainersConfiguration.class,
    JpaAreaRepository.class,
    JpaTagRepository.class,
    JpaQuestionRepository.class,
    JpaChallengeRepository.class
})
class JpaCatalogRepositoriesTest {

    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaTagRepository tags;
    @Autowired private JpaQuestionRepository questions;
    @Autowired private JpaChallengeRepository challenges;
    @Autowired private SpringDataQuestionRepository questionRows;
    @Autowired private TestEntityManager entityManager;

    @Test
    void questionGetsItsCreationTimeFromTheDatabase() {
        Area area =
                areas.save(
                        new Area(
                                UUID.randomUUID(),
                                "zz-created-" + UUID.randomUUID().toString().substring(0, 8),
                                "Área",
                                new SessionConfig(8, 2, 45, 0.5),
                                true));
        Question question =
                questions.save(
                        new Question(
                                UUID.randomUUID(),
                                "q-created-" + UUID.randomUUID().toString().substring(0, 8),
                                "Título",
                                area.id(),
                                Level.JUNIOR,
                                Difficulty.EASY,
                                "Enunciado",
                                "Explicação",
                                List.of(
                                        new QuestionOption(UUID.randomUUID(), "A", true, 0),
                                        new QuestionOption(UUID.randomUUID(), "B", false, 1)),
                                Set.of(),
                                true));
        entityManager.clear();

        assertThat(questionRows.findById(question.id()).orElseThrow().getCreatedAt()).isNotNull();
    }

    @Test
    void persistsTheWholeCatalogGraph() {
        Area backend =
                areas.save(
                        new Area(
                                UUID.randomUUID(),
                                "zz-backend-" + UUID.randomUUID().toString().substring(0, 8),
                                "Backend",
                                new SessionConfig(8, 2, 45, 0.5),
                                true));
        Area inactive =
                areas.save(
                        new Area(
                                UUID.randomUUID(),
                                "zz-old-" + UUID.randomUUID().toString().substring(0, 8),
                                "Antiga",
                                new SessionConfig(8, 2, 45, 0.5),
                                false));
        String tagSlug = "t-" + UUID.randomUUID().toString().substring(0, 8);
        tags.save(new Tag(UUID.randomUUID(), tagSlug, "Tag"));

        assertThat(areas.findAllActive())
                .extracting(Area::id)
                .contains(backend.id())
                .doesNotContain(inactive.id());
        assertThat(areas.findBySlug(backend.slug())).map(Area::name).contains("Backend");
        assertThat(tags.findBySlug(tagSlug)).isPresent();

        String qSlug = "q-" + UUID.randomUUID().toString().substring(0, 8);
        Question saved =
                questions.save(
                        new Question(
                                UUID.randomUUID(),
                                qSlug,
                                "Título",
                                backend.id(),
                                Level.JUNIOR,
                                Difficulty.EASY,
                                "Enunciado",
                                "Explicação",
                                List.of(
                                        new QuestionOption(UUID.randomUUID(), "A", false, 0),
                                        new QuestionOption(UUID.randomUUID(), "B", true, 1)),
                                Set.of(tagSlug),
                                true));
        assertThat(questions.existsBySlug(qSlug)).isTrue();
        assertThat(saved.correctOption().text()).isEqualTo("B");
        assertThat(saved.tagSlugs()).containsExactly(tagSlug);
        assertThat(questions.countActiveByAreaAndLevel(backend.id(), Level.JUNIOR)).isEqualTo(1);
        assertThat(questions.countActiveByAreaAndLevel(backend.id(), Level.PLENO)).isZero();

        String cSlug = "c-" + UUID.randomUUID().toString().substring(0, 8);
        Challenge challenge =
                challenges.save(
                        new Challenge(
                                UUID.randomUUID(),
                                cSlug,
                                "Título",
                                backend.id(),
                                Level.JUNIOR,
                                Difficulty.MEDIUM,
                                "Enunciado",
                                "Abordagem",
                                new ExecutionLimits(5000, 262144),
                                ComparisonMode.EXACT,
                                5,
                                Map.of(
                                        Language.JAVA,
                                        new ChallengeLanguage(
                                                Language.JAVA, "f", "class Solution {}", "ref")),
                                List.of(
                                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                                Set.of(tagSlug),
                                true));
        assertThat(challenges.existsBySlug(cSlug)).isTrue();
        assertThat(challenge.languages().get(Language.JAVA).referenceSolution()).isEqualTo("ref");
        assertThat(challenge.testCases())
                .extracting(TestCase::position)
                .containsExactly(0, 1, 2, 3);
        assertThat(challenge.hiddenTestCases()).hasSize(2);
        assertThat(challenges.countActiveByAreaAndLevel(backend.id(), Level.JUNIOR)).isEqualTo(1);
        entityManager.clear();
        assertThat(questions.findBySlug(qSlug)).map(Question::title).contains("Título");
        assertThat(questions.findBySlug(qSlug)).map(Question::difficulty).contains(Difficulty.EASY);
        assertThat(challenges.findBySlug(cSlug))
                .map(Challenge::difficulty)
                .contains(Difficulty.MEDIUM);
        assertThat(questions.findBySlug("nao-existe")).isEmpty();
    }

    @Test
    void updatesChildrenInPlaceWithoutViolatingUniqueConstraints() {
        Area area =
                areas.save(
                        new Area(
                                UUID.randomUUID(),
                                "zz-upd-" + UUID.randomUUID().toString().substring(0, 8),
                                "Atualização",
                                new SessionConfig(8, 2, 45, 0.5),
                                true));
        String qSlug = "q-upd-" + UUID.randomUUID().toString().substring(0, 8);
        Question original =
                questions.save(
                        new Question(
                                UUID.randomUUID(),
                                qSlug,
                                "Título",
                                area.id(),
                                Level.JUNIOR,
                                Difficulty.EASY,
                                "Enunciado",
                                "Explicação",
                                List.of(
                                        new QuestionOption(UUID.randomUUID(), "A", true, 0),
                                        new QuestionOption(UUID.randomUUID(), "B", false, 1)),
                                Set.of(),
                                true));

        Question edited =
                new Question(
                        original.id(),
                        qSlug,
                        "Título 2",
                        area.id(),
                        Level.PLENO,
                        Difficulty.HARD,
                        "Enunciado 2",
                        "Explicação 2",
                        List.of(
                                new QuestionOption(UUID.randomUUID(), "C", false, 0),
                                new QuestionOption(UUID.randomUUID(), "D", true, 1),
                                new QuestionOption(UUID.randomUUID(), "E", false, 2)),
                        Set.of(),
                        false);
        Question saved = questions.save(edited);

        assertThat(saved.title()).isEqualTo("Título 2");
        assertThat(saved.difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(saved.level()).isEqualTo(Level.PLENO);
        assertThat(saved.options()).extracting(QuestionOption::text).containsExactly("C", "D", "E");
        assertThat(saved.correctOption().text()).isEqualTo("D");
        assertThat(questions.countActiveByAreaAndLevel(area.id(), Level.PLENO)).isZero();

        String cSlug = "c-upd-" + UUID.randomUUID().toString().substring(0, 8);
        Challenge first =
                challenges.save(
                        new Challenge(
                                UUID.randomUUID(),
                                cSlug,
                                "Título",
                                area.id(),
                                Level.JUNIOR,
                                Difficulty.MEDIUM,
                                "Enunciado",
                                "Abordagem",
                                new ExecutionLimits(5000, 262144),
                                ComparisonMode.EXACT,
                                5,
                                Map.of(
                                        Language.JAVA,
                                        new ChallengeLanguage(
                                                Language.JAVA, "f", "class Solution {}", null)),
                                List.of(
                                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                                Set.of(),
                                true));
        Challenge editedChallenge =
                new Challenge(
                        first.id(),
                        cSlug,
                        "Título 2",
                        area.id(),
                        Level.JUNIOR,
                        Difficulty.MEDIUM,
                        "Enunciado",
                        "Abordagem",
                        new ExecutionLimits(3000, 131072),
                        ComparisonMode.UNORDERED,
                        3,
                        Map.of(
                                Language.JAVA,
                                new ChallengeLanguage(
                                        Language.JAVA, "g", "class Solution { }", "ref"),
                                Language.PYTHON,
                                new ChallengeLanguage(
                                        Language.PYTHON, "g", "class Solution: pass", "ref")),
                        List.of(
                                new TestCase(UUID.randomUUID(), 0, "[9]", "9", true),
                                new TestCase(UUID.randomUUID(), 1, "[8]", "8", true),
                                new TestCase(UUID.randomUUID(), 2, "[7]", "7", false),
                                new TestCase(UUID.randomUUID(), 3, "[6]", "6", false)),
                        Set.of(),
                        true);
        Challenge savedChallenge = challenges.save(editedChallenge);

        assertThat(savedChallenge.title()).isEqualTo("Título 2");
        assertThat(savedChallenge.languages()).containsOnlyKeys(Language.JAVA, Language.PYTHON);
        assertThat(savedChallenge.languages().get(Language.JAVA).functionName()).isEqualTo("g");
        assertThat(savedChallenge.testCases())
                .extracting(TestCase::argsJson)
                .containsExactly("[9]", "[8]", "[7]", "[6]");
        assertThat(savedChallenge.maxSubmissions()).isEqualTo(3);
    }

    @Test
    void resavingWithANewTitleAndDifficultyKeepsTheChildren() {
        Area area =
                areas.save(
                        new Area(
                                UUID.randomUUID(),
                                "zz-title-" + UUID.randomUUID().toString().substring(0, 8),
                                "Título",
                                new SessionConfig(8, 2, 45, 0.5),
                                true));
        String qSlug = "q-title-" + UUID.randomUUID().toString().substring(0, 8);
        questions.save(
                new Question(
                        UUID.randomUUID(),
                        qSlug,
                        qSlug,
                        area.id(),
                        Level.JUNIOR,
                        Difficulty.MEDIUM,
                        "Enunciado",
                        "Explicação",
                        List.of(
                                new QuestionOption(UUID.randomUUID(), "A", true, 0),
                                new QuestionOption(UUID.randomUUID(), "B", false, 1)),
                        Set.of(),
                        true));
        String cSlug = "c-title-" + UUID.randomUUID().toString().substring(0, 8);
        challenges.save(
                new Challenge(
                        UUID.randomUUID(),
                        cSlug,
                        "Desafio",
                        area.id(),
                        Level.JUNIOR,
                        Difficulty.MEDIUM,
                        "Enunciado",
                        "Abordagem",
                        new ExecutionLimits(5000, 262144),
                        ComparisonMode.EXACT,
                        5,
                        Map.of(
                                Language.JAVA,
                                new ChallengeLanguage(
                                        Language.JAVA, "f", "class Solution {}", "ref")),
                        List.of(
                                new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                                new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                                new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                                new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                        Set.of(),
                        true));
        entityManager.flush();
        entityManager.clear();

        Question loaded = questions.findBySlug(qSlug).orElseThrow();
        questions.save(
                new Question(
                        loaded.id(),
                        loaded.slug(),
                        "Título de verdade",
                        loaded.areaId(),
                        loaded.level(),
                        Difficulty.HARD,
                        loaded.statement(),
                        loaded.explanation(),
                        loaded.options(),
                        loaded.tagSlugs(),
                        loaded.active()));
        Challenge loadedChallenge = challenges.findBySlug(cSlug).orElseThrow();
        challenges.save(
                new Challenge(
                        loadedChallenge.id(),
                        loadedChallenge.slug(),
                        "Desafio de verdade",
                        loadedChallenge.areaId(),
                        loadedChallenge.level(),
                        Difficulty.EASY,
                        loadedChallenge.statement(),
                        loadedChallenge.approachSummary(),
                        loadedChallenge.limits(),
                        loadedChallenge.comparisonMode(),
                        loadedChallenge.maxSubmissions(),
                        loadedChallenge.languages(),
                        loadedChallenge.testCases(),
                        loadedChallenge.tagSlugs(),
                        loadedChallenge.active()));
        entityManager.flush();
        entityManager.clear();

        Question saved = questions.findBySlug(qSlug).orElseThrow();
        assertThat(saved.title()).isEqualTo("Título de verdade");
        assertThat(saved.difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(saved.options())
                .extracting(QuestionOption::id)
                .containsExactlyElementsOf(
                        loaded.options().stream().map(QuestionOption::id).toList());
        Challenge savedChallenge = challenges.findBySlug(cSlug).orElseThrow();
        assertThat(savedChallenge.title()).isEqualTo("Desafio de verdade");
        assertThat(savedChallenge.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(savedChallenge.testCases()).hasSize(4);
    }
}
