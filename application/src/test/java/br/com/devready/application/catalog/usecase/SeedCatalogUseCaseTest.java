package br.com.devready.application.catalog.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.catalog.dto.CatalogSeed;
import br.com.devready.application.catalog.dto.ChallengeSeed;
import br.com.devready.application.catalog.dto.QuestionSeed;
import br.com.devready.application.catalog.dto.SeedReport;
import br.com.devready.application.catalog.exception.InvalidSeedException;
import br.com.devready.application.catalog.fake.CatalogSeedFixtures;
import br.com.devready.application.catalog.fake.InMemoryAreaRepository;
import br.com.devready.application.catalog.fake.InMemoryChallengeRepository;
import br.com.devready.application.catalog.fake.InMemoryQuestionRepository;
import br.com.devready.application.catalog.fake.InMemoryTagRepository;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.catalog.CatalogException;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.InvalidChallengeException;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SeedCatalogUseCaseTest {

    private final InMemoryAreaRepository areas = new InMemoryAreaRepository();
    private final InMemoryTagRepository tags = new InMemoryTagRepository();
    private final InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
    private final InMemoryChallengeRepository challenges = new InMemoryChallengeRepository();

    private SeedCatalogUseCase seeder(CatalogSeed seed) {
        return new SeedCatalogUseCase(
                () -> seed, areas, tags, questions, challenges, new SequentialIdGenerator());
    }

    @Test
    void insertsEverythingOnceAndIsIdempotent() {
        SeedCatalogUseCase seeder = seeder(CatalogSeedFixtures.seed());

        SeedReport first = seeder.execute();
        SeedReport second = seeder.execute();

        assertThat(first).isEqualTo(new SeedReport(1, 1, 1, 1, 0, 0));
        assertThat(second.total()).isZero();
        var area = areas.findBySlug("backend").orElseThrow();
        assertThat(area.sessionConfig().durationMinutes()).isEqualTo(45);
        var question = questions.get("backend-junior-http-idempotency");
        assertThat(question.areaId()).isEqualTo(area.id());
        assertThat(question.correctOption().text()).isEqualTo("PUT");
        assertThat(question.options()).extracting(o -> o.position()).containsExactly(0, 1, 2);
        assertThat(question.tagSlugs()).containsExactly("http");
        var challenge = challenges.get("two-sum");
        assertThat(challenge.languages().get(Language.JAVA).functionName()).isEqualTo("twoSum");
        assertThat(challenge.visibleTestCases()).hasSize(2);
        assertThat(questions.countActiveByAreaAndLevel(area.id(), Level.JUNIOR)).isEqualTo(1);
    }

    @Test
    void doesNotOverwriteExistingContent() {
        seeder(CatalogSeedFixtures.seed()).execute();
        var edited =
                new QuestionSeed(
                        "backend-junior-http-idempotency",
                        "Idempotência dos métodos HTTP",
                        "backend",
                        Level.JUNIOR,
                        Difficulty.EASY,
                        "Outro enunciado",
                        "x",
                        CatalogSeedFixtures.idempotency().options(),
                        List.of());
        var seed =
                new CatalogSeed(
                        List.of(CatalogSeedFixtures.backend()),
                        List.of(CatalogSeedFixtures.http()),
                        List.of(edited),
                        List.of());

        seeder(seed).execute();

        assertThat(questions.get("backend-junior-http-idempotency").statement())
                .startsWith("Qual método");
    }

    @Test
    void updatesOnlyTitleAndDifficultyOfExistingItems() {
        seeder(CatalogSeedFixtures.seed()).execute();
        QuestionSeed original = CatalogSeedFixtures.idempotency();
        UUID questionId = questions.get(original.slug()).id();
        var optionsBefore = questions.get(original.slug()).options();
        var casesBefore = challenges.get(CatalogSeedFixtures.twoSum().slug()).testCases();
        int questionSaves = questions.saves();
        int challengeSaves = challenges.saves();
        QuestionSeed retitled =
                new QuestionSeed(
                        original.slug(),
                        "Métodos HTTP idempotentes",
                        original.areaSlug(),
                        original.level(),
                        Difficulty.MEDIUM,
                        "Outro enunciado",
                        "Outra explicação",
                        original.options(),
                        List.of());
        ChallengeSeed twoSum = CatalogSeedFixtures.twoSum();
        ChallengeSeed harder =
                new ChallengeSeed(
                        twoSum.slug(),
                        "Outro título",
                        twoSum.areaSlug(),
                        twoSum.level(),
                        Difficulty.HARD,
                        "Outro enunciado",
                        twoSum.approachSummary(),
                        twoSum.timeLimitMs(),
                        twoSum.memoryLimitKb(),
                        twoSum.comparison(),
                        twoSum.maxSubmissions(),
                        twoSum.languages(),
                        twoSum.testCases(),
                        twoSum.tags());
        CatalogSeed edited =
                new CatalogSeed(
                        List.of(CatalogSeedFixtures.backend()),
                        List.of(CatalogSeedFixtures.http()),
                        List.of(retitled),
                        List.of(harder));

        SeedReport report = seeder(edited).execute();

        assertThat(report).isEqualTo(new SeedReport(0, 0, 0, 0, 1, 1));
        var question = questions.get(original.slug());
        assertThat(question.id()).isEqualTo(questionId);
        assertThat(question.title()).isEqualTo("Métodos HTTP idempotentes");
        assertThat(question.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(question.statement()).startsWith("Qual método");
        assertThat(question.tagSlugs()).containsExactly("http");
        var challenge = challenges.get(twoSum.slug());
        assertThat(challenge.title()).isEqualTo("Outro título");
        assertThat(challenge.difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(challenge.statement()).startsWith("Dado um array");
        assertThat(questions.saves()).isEqualTo(questionSaves);
        assertThat(challenges.saves()).isEqualTo(challengeSaves);
        assertThat(question.options()).isEqualTo(optionsBefore);
        assertThat(challenge.testCases()).isEqualTo(casesBefore);
        assertThat(seeder(edited).execute().total()).isZero();
    }

    @Test
    void refusesABlankTitleWhenRefreshing() {
        seeder(CatalogSeedFixtures.seed()).execute();
        QuestionSeed question = CatalogSeedFixtures.idempotency();

        assertThatThrownBy(
                        () ->
                                seeder(
                                                displayed(
                                                        questionWith(
                                                                question,
                                                                " ",
                                                                question.difficulty()),
                                                        CatalogSeedFixtures.twoSum()))
                                        .execute())
                .isInstanceOf(CatalogException.class);
        assertThat(questions.get(question.slug()).title()).isEqualTo(question.title());
    }

    @Test
    void updatesWhenOnlyTheDifficultyChanges() {
        seeder(CatalogSeedFixtures.seed()).execute();
        QuestionSeed question = CatalogSeedFixtures.idempotency();
        ChallengeSeed challenge = CatalogSeedFixtures.twoSum();

        SeedReport report =
                seeder(
                                displayed(
                                        questionWith(question, question.title(), Difficulty.HARD),
                                        challengeWith(
                                                challenge, challenge.title(), Difficulty.HARD)))
                        .execute();

        assertThat(report.updatedQuestions()).isEqualTo(1);
        assertThat(report.updatedChallenges()).isEqualTo(1);
        assertThat(questions.get(question.slug()).difficulty()).isEqualTo(Difficulty.HARD);
        assertThat(challenges.get(challenge.slug()).difficulty()).isEqualTo(Difficulty.HARD);
    }

    @Test
    void updatesWhenOnlyTheTitleChanges() {
        seeder(CatalogSeedFixtures.seed()).execute();
        QuestionSeed question = CatalogSeedFixtures.idempotency();
        ChallengeSeed challenge = CatalogSeedFixtures.twoSum();

        SeedReport report =
                seeder(
                                displayed(
                                        questionWith(
                                                question, "Outro título", question.difficulty()),
                                        challengeWith(
                                                challenge, "Outro título", challenge.difficulty())))
                        .execute();

        assertThat(report.updatedQuestions()).isEqualTo(1);
        assertThat(report.updatedChallenges()).isEqualTo(1);
        assertThat(questions.get(question.slug()).title()).isEqualTo("Outro título");
        assertThat(challenges.get(challenge.slug()).title()).isEqualTo("Outro título");
    }

    @Test
    void updatesTheSubmissionLimitOfAnExistingChallenge() {
        seeder(CatalogSeedFixtures.seed()).execute();
        QuestionSeed question = CatalogSeedFixtures.idempotency();
        ChallengeSeed challenge = CatalogSeedFixtures.twoSum();
        int challengeSaves = challenges.saves();

        SeedReport report = seeder(displayed(question, challengeWithLimit(challenge, 3))).execute();

        assertThat(report.updatedChallenges()).isEqualTo(1);
        assertThat(challenges.get(challenge.slug()).maxSubmissions()).isEqualTo(3);
        assertThat(challenges.saves()).isEqualTo(challengeSaves);
    }

    @Test
    void refusesASubmissionLimitAboveFive() {
        ChallengeSeed challenge = CatalogSeedFixtures.twoSum();

        assertThatThrownBy(
                        () ->
                                seeder(
                                                displayed(
                                                        CatalogSeedFixtures.idempotency(),
                                                        challengeWithLimit(challenge, 6)))
                                        .execute())
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("entre 1 e 5");
        assertThat(challenges.existsBySlug(challenge.slug())).isFalse();

        seeder(CatalogSeedFixtures.seed()).execute();
        assertThatThrownBy(
                        () ->
                                seeder(
                                                displayed(
                                                        CatalogSeedFixtures.idempotency(),
                                                        challengeWithLimit(challenge, 6)))
                                        .execute())
                .isInstanceOf(InvalidChallengeException.class);
        assertThat(challenges.get(challenge.slug()).maxSubmissions())
                .isEqualTo(challenge.maxSubmissions());
    }

    @Test
    void resolvesAreasAndTagsAlreadyInTheRepositoryWhenTheRunOmitsThem() {
        seeder(CatalogSeedFixtures.seed()).execute();

        var extraQuestion =
                new QuestionSeed(
                        "backend-junior-extra",
                        "Questão extra",
                        "backend",
                        Level.JUNIOR,
                        Difficulty.MEDIUM,
                        "Outro enunciado",
                        "outra explicação",
                        CatalogSeedFixtures.idempotency().options(),
                        List.of("http"));
        var twoSum = CatalogSeedFixtures.twoSum();
        var extraChallenge =
                new ChallengeSeed(
                        "two-sum-extra",
                        "Soma de dois números (extra)",
                        twoSum.areaSlug(),
                        twoSum.level(),
                        twoSum.difficulty(),
                        twoSum.statement(),
                        twoSum.approachSummary(),
                        twoSum.timeLimitMs(),
                        twoSum.memoryLimitKb(),
                        twoSum.comparison(),
                        twoSum.maxSubmissions(),
                        twoSum.languages(),
                        twoSum.testCases(),
                        twoSum.tags());
        var second =
                new CatalogSeed(
                        List.of(), List.of(), List.of(extraQuestion), List.of(extraChallenge));

        SeedReport report = seeder(second).execute();

        assertThat(report).isEqualTo(new SeedReport(0, 0, 1, 1, 0, 0));
        var area = areas.findBySlug("backend").orElseThrow();
        assertThat(questions.get("backend-junior-extra").areaId()).isEqualTo(area.id());
        assertThat(challenges.existsBySlug("two-sum-extra")).isTrue();
    }

    @Test
    void rejectsUnknownAreaOrTag() {
        var badArea =
                new QuestionSeed(
                        "q-1",
                        "Q",
                        "mobile",
                        Level.JUNIOR,
                        Difficulty.EASY,
                        "s",
                        "e",
                        CatalogSeedFixtures.idempotency().options(),
                        List.of());
        assertThatThrownBy(
                        () ->
                                seeder(
                                                new CatalogSeed(
                                                        List.of(CatalogSeedFixtures.backend()),
                                                        List.of(),
                                                        List.of(badArea),
                                                        List.of()))
                                        .execute())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("mobile");

        var badTag =
                new QuestionSeed(
                        "q-2",
                        "Q",
                        "backend",
                        Level.JUNIOR,
                        Difficulty.EASY,
                        "s",
                        "e",
                        CatalogSeedFixtures.idempotency().options(),
                        List.of("nao-existe"));
        assertThatThrownBy(
                        () ->
                                seeder(
                                                new CatalogSeed(
                                                        List.of(CatalogSeedFixtures.backend()),
                                                        List.of(),
                                                        List.of(badTag),
                                                        List.of()))
                                        .execute())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("nao-existe");
    }

    @Test
    void rejectsChallengeWithUnknownAreaOrTag() {
        var twoSum = CatalogSeedFixtures.twoSum();
        var badArea = challengeLike(twoSum, "c-1", "mobile", twoSum.tags());
        assertThatThrownBy(
                        () ->
                                seeder(
                                                new CatalogSeed(
                                                        List.of(CatalogSeedFixtures.backend()),
                                                        List.of(),
                                                        List.of(),
                                                        List.of(badArea)))
                                        .execute())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("mobile");

        var badTag = challengeLike(twoSum, "c-2", "backend", List.of("nao-existe"));
        assertThatThrownBy(
                        () ->
                                seeder(
                                                new CatalogSeed(
                                                        List.of(CatalogSeedFixtures.backend()),
                                                        List.of(),
                                                        List.of(),
                                                        List.of(badTag)))
                                        .execute())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("nao-existe");
    }

    private static CatalogSeed displayed(QuestionSeed question, ChallengeSeed challenge) {
        return new CatalogSeed(
                List.of(CatalogSeedFixtures.backend()),
                List.of(CatalogSeedFixtures.http()),
                List.of(question),
                List.of(challenge));
    }

    private static QuestionSeed questionWith(
            QuestionSeed base, String title, Difficulty difficulty) {
        return new QuestionSeed(
                base.slug(),
                title,
                base.areaSlug(),
                base.level(),
                difficulty,
                base.statement(),
                base.explanation(),
                base.options(),
                base.tags());
    }

    private static ChallengeSeed challengeWith(
            ChallengeSeed base, String title, Difficulty difficulty) {
        return new ChallengeSeed(
                base.slug(),
                title,
                base.areaSlug(),
                base.level(),
                difficulty,
                base.statement(),
                base.approachSummary(),
                base.timeLimitMs(),
                base.memoryLimitKb(),
                base.comparison(),
                base.maxSubmissions(),
                base.languages(),
                base.testCases(),
                base.tags());
    }

    private static ChallengeSeed challengeWithLimit(ChallengeSeed base, int maxSubmissions) {
        return new ChallengeSeed(
                base.slug(),
                base.title(),
                base.areaSlug(),
                base.level(),
                base.difficulty(),
                base.statement(),
                base.approachSummary(),
                base.timeLimitMs(),
                base.memoryLimitKb(),
                base.comparison(),
                maxSubmissions,
                base.languages(),
                base.testCases(),
                base.tags());
    }

    private static ChallengeSeed challengeLike(
            ChallengeSeed base, String slug, String areaSlug, List<String> tags) {
        return new ChallengeSeed(
                slug,
                base.title(),
                areaSlug,
                base.level(),
                base.difficulty(),
                base.statement(),
                base.approachSummary(),
                base.timeLimitMs(),
                base.memoryLimitKb(),
                base.comparison(),
                base.maxSubmissions(),
                base.languages(),
                base.testCases(),
                tags);
    }
}
