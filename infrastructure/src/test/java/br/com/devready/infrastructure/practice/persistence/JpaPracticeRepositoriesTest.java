package br.com.devready.infrastructure.practice.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.ChallengeLanguage;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.ExecutionLimits;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.ProblemType;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.catalog.Tag;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.practice.Answer;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.catalog.persistence.JpaAreaRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaChallengeRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaPracticeCatalog;
import br.com.devready.infrastructure.catalog.persistence.JpaQuestionRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaTagRepository;
import br.com.devready.infrastructure.identity.persistence.JpaUserRepository;
import java.time.Instant;
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

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    PostgresTestcontainersConfiguration.class,
    JpaAreaRepository.class,
    JpaTagRepository.class,
    JpaQuestionRepository.class,
    JpaChallengeRepository.class,
    JpaPracticeCatalog.class,
    JpaUserRepository.class,
    JpaAnswerRepository.class,
    JpaQuestionProgressReader.class
})
class JpaPracticeRepositoriesTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaTagRepository tags;
    @Autowired private JpaQuestionRepository questions;
    @Autowired private JpaChallengeRepository challenges;
    @Autowired private JpaPracticeCatalog catalog;
    @Autowired private JpaUserRepository users;
    @Autowired private JpaAnswerRepository answers;
    @Autowired private JpaQuestionProgressReader progress;
    @Autowired private TestEntityManager entityManager;

    private record Fixture(
            Area area, Question sql, Question http, Question extra, Challenge challenge) {}

    @Test
    void keepsEveryAnswerAndReadsThemOldestFirst() {
        Fixture f = fixture();
        UUID ana = user("ana");
        UUID bia = user("bia");
        Answer late =
                answers.save(answer(new UUID(0, 1), ana, f.sql(), false, 0, T0.plusSeconds(60)));
        Answer http =
                answers.save(answer(new UUID(0, 2), ana, f.http(), false, 0, T0.plusSeconds(30)));
        Answer early = answers.save(answer(new UUID(0, 3), ana, f.sql(), true, 20, T0));
        Answer biaAnswer =
                answers.save(answer(new UUID(0, 4), bia, f.sql(), true, 20, T0.plusSeconds(10)));
        entityManager.clear();

        assertThat(answers.findByUser(ana)).containsExactly(early, http, late);
        assertThat(answers.findByUserAndQuestion(ana, f.sql().id())).containsExactly(early, late);
        assertThat(answers.findByQuestions(List.of(f.sql().id())))
                .containsExactly(early, biaAnswer, late);
        assertThat(answers.findByQuestions(List.of())).isEmpty();
        assertThat(answers.findByUser(UUID.randomUUID())).isEmpty();
    }

    @Test
    void answersAtTheSameInstantComeBackByAscendingId() {
        Fixture f = fixture();
        UUID ana = user("empate");
        Answer second =
                answers.save(answer(new UUID(0, 6), ana, f.extra(), false, 0, T0.plusSeconds(5)));
        Answer first =
                answers.save(answer(new UUID(0, 5), ana, f.extra(), false, 0, T0.plusSeconds(5)));
        entityManager.clear();

        assertThat(answers.findByUser(ana)).containsExactly(first, second);
        assertThat(answers.findByUserAndQuestion(ana, f.extra().id()))
                .containsExactly(first, second);
        assertThat(answers.findByQuestions(List.of(f.extra().id()))).containsExactly(first, second);
    }

    @Test
    void onlyOneAnswerPerPersonAndQuestionEarnsXp() {
        Fixture f = fixture();
        UUID ana = user("xp");
        answers.save(answer(ana, f.sql(), true, 20, T0));
        answers.save(answer(ana, f.sql(), true, 0, T0.plusSeconds(1)));
        answers.save(answer(ana, f.http(), true, 20, T0.plusSeconds(1)));

        assertThatThrownBy(() -> answers.save(answer(ana, f.sql(), true, 20, T0.plusSeconds(2))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void anAnswerPointsToAnOptionThatExists() {
        Fixture f = fixture();
        UUID ana = user("opcao");

        assertThatThrownBy(
                        () ->
                                answers.save(
                                        new Answer(
                                                UUID.randomUUID(),
                                                ana,
                                                f.sql().id(),
                                                UUID.randomUUID(),
                                                false,
                                                0,
                                                T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theProgressReaderTellsSolvedFromAttemptedBySlug() {
        Fixture f = fixture();
        UUID ana = user("andamento");
        UUID bia = user("outra");
        answers.save(answer(ana, f.sql(), false, 0, T0));
        answers.save(answer(ana, f.sql(), true, 20, T0.plusSeconds(1)));
        answers.save(answer(ana, f.http(), false, 0, T0.plusSeconds(2)));
        answers.save(answer(bia, f.extra(), true, 20, T0));
        entityManager.clear();

        Map<String, QuestionProgress> answered = progress.answeredBy(ana);

        assertThat(answered)
                .containsOnly(
                        entry(f.sql().slug(), QuestionProgress.SOLVED),
                        entry(f.http().slug(), QuestionProgress.ATTEMPTED));
        assertThat(progress.answeredBy(UUID.randomUUID())).isEmpty();
    }

    @Test
    void updatingTheDisplayKeepsOptionsTestCasesAndAnswers() {
        Fixture f = fixture();
        UUID ana = user("titulo");
        Answer kept = answers.save(answer(ana, f.sql(), true, 20, T0));
        entityManager.clear();

        questions.updateDisplay(f.sql().id(), "Outro título", Difficulty.HARD);
        challenges.updateDisplay(f.challenge().id(), "Outro desafio", Difficulty.MEDIUM, 2);
        entityManager.clear();

        Question question = questions.findBySlug(f.sql().slug()).orElseThrow();
        assertThat(question.title()).isEqualTo("Outro título");
        assertThat(question.difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(question.options())
                .extracting(QuestionOption::id)
                .containsExactlyElementsOf(
                        f.sql().options().stream().map(QuestionOption::id).toList());
        Challenge challenge = challenges.findBySlug(f.challenge().slug()).orElseThrow();
        assertThat(challenge.title()).isEqualTo("Outro desafio");
        assertThat(challenge.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(challenge.maxSubmissions()).isEqualTo(2);
        assertThat(challenge.testCases())
                .extracting(TestCase::id)
                .containsExactlyInAnyOrderElementsOf(
                        f.challenge().testCases().stream().map(TestCase::id).toList());
        assertThat(answers.findByUser(ana)).containsExactly(kept);
    }

    @Test
    void activeProblemsBringTheQuestionsAndChallengesOfTheAreaWithTheirTags() {
        Fixture f = fixture();
        entityManager.clear();

        List<ProblemSummary> problems =
                catalog.activeProblems(f.area().id(), Level.upTo(Level.SENIOR));

        assertThat(problems)
                .extracting(ProblemSummary::slug)
                .containsExactlyInAnyOrder(
                        f.sql().slug(), f.http().slug(), f.extra().slug(), f.challenge().slug());
        ProblemSummary sql = find(problems, f.sql().slug());
        assertThat(sql.id()).isEqualTo(f.sql().id());
        assertThat(sql.type()).isEqualTo(ProblemType.QUESTION);
        assertThat(sql.title()).isEqualTo(f.sql().title());
        assertThat(sql.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(sql.level()).isEqualTo(f.sql().level());
        assertThat(sql.tagSlugs()).containsExactlyInAnyOrder("zz-sql", "zz-banco");
        ProblemSummary challenge = find(problems, f.challenge().slug());
        assertThat(challenge.type()).isEqualTo(ProblemType.CHALLENGE);
        assertThat(challenge.tagSlugs()).containsExactly("zz-sql");
        assertThat(catalog.activeProblems(f.area().id(), Set.of())).isEmpty();
    }

    @Test
    void findsAChallengeByIdWithItsLanguagesAndCasesInOrder() {
        Fixture f = fixture();
        entityManager.clear();

        Challenge found = challenges.findById(f.challenge().id()).orElseThrow();

        assertThat(found.slug()).isEqualTo(f.challenge().slug());
        assertThat(found.testCases()).extracting(TestCase::position).containsExactly(0, 1, 2, 3);
        assertThat(found.languages()).containsKey(Language.JAVA);
        assertThat(challenges.findById(UUID.randomUUID())).isEmpty();
    }

    private Fixture fixture() {
        Area area = areas.save(area("zz-pratica"));
        Area other = areas.save(area("zz-outra"));
        tags.save(new Tag(UUID.randomUUID(), "zz-sql", "SQL"));
        tags.save(new Tag(UUID.randomUUID(), "zz-banco", "Banco"));
        tags.save(new Tag(UUID.randomUUID(), "zz-http", "HTTP"));
        Question sql =
                questions.save(question("zz-q-sql", "Junção", area, true, "zz-sql", "zz-banco"));
        Question http =
                questions.save(question("zz-q-http", "Idempotência", area, true, "zz-http"));
        Question extra = questions.save(question("zz-q-extra", "Índice", area, true, "zz-sql"));
        questions.save(question("zz-q-inativa", "Inativa", area, false, "zz-sql"));
        questions.save(question("zz-q-outra", "Outra área", other, true, "zz-sql"));
        Challenge challenge = challenges.save(challenge("zz-c-soma", area, "zz-sql", true));
        challenges.save(challenge("zz-c-inativo", area, "zz-sql", false));
        challenges.save(challenge("zz-c-outra", other, "zz-sql", true));
        return new Fixture(area, sql, http, extra, challenge);
    }

    private UUID user(String name) {
        UUID id = UUID.randomUUID();
        users.save(User.register(id, name + "@example.com", name, "hash", T0));
        return id;
    }

    private static Answer answer(
            UUID user, Question question, boolean correct, int xp, Instant at) {
        return answer(UUID.randomUUID(), user, question, correct, xp, at);
    }

    private static Answer answer(
            UUID id, UUID user, Question question, boolean correct, int xp, Instant at) {
        QuestionOption option =
                question.options().stream()
                        .filter(candidate -> candidate.correct() == correct)
                        .findFirst()
                        .orElseThrow();
        return new Answer(id, user, question.id(), option.id(), correct, xp, at);
    }

    private static ProblemSummary find(List<ProblemSummary> problems, String slug) {
        return problems.stream().filter(p -> p.slug().equals(slug)).findFirst().orElseThrow();
    }

    private static Area area(String slug) {
        return new Area(
                UUID.randomUUID(), slug, "Área " + slug, new SessionConfig(8, 2, 45, 0.5), true);
    }

    private static Question question(
            String slug, String title, Area area, boolean active, String... tagSlugs) {
        return new Question(
                UUID.randomUUID(),
                slug,
                title,
                area.id(),
                Level.JUNIOR,
                Difficulty.EASY,
                "Enunciado de " + slug,
                "Explicação de " + slug,
                List.of(
                        new QuestionOption(UUID.randomUUID(), "A", false, 0),
                        new QuestionOption(UUID.randomUUID(), "B", true, 1)),
                Set.of(tagSlugs),
                active);
    }

    private static Challenge challenge(String slug, Area area, String tagSlug, boolean active) {
        return new Challenge(
                UUID.randomUUID(),
                slug,
                "Soma",
                area.id(),
                Level.JUNIOR,
                Difficulty.EASY,
                "Enunciado",
                "Abordagem",
                new ExecutionLimits(5000, 262144),
                ComparisonMode.EXACT,
                5,
                Map.of(
                        Language.JAVA,
                        new ChallengeLanguage(Language.JAVA, "soma", "class Solution {}", "ref")),
                List.of(
                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                Set.of(tagSlug),
                active);
    }
}
