package br.com.devready.infrastructure.evaluation.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import br.com.devready.application.evaluation.exception.XpAlreadyGrantedException;
import br.com.devready.domain.career.QuestionProgress;
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
import br.com.devready.domain.evaluation.CaseResult;
import br.com.devready.domain.evaluation.CaseStatus;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.evaluation.Verdict;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.practice.Answer;
import br.com.devready.domain.practice.ChallengeAttempt;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.catalog.persistence.JpaAreaRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaChallengeRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaQuestionRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaTagRepository;
import br.com.devready.infrastructure.identity.persistence.JpaUserRepository;
import br.com.devready.infrastructure.practice.persistence.JpaAnswerRepository;
import br.com.devready.infrastructure.practice.persistence.JpaChallengeAttemptReader;
import br.com.devready.infrastructure.practice.persistence.JpaQuestionProgressReader;
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
    JpaUserRepository.class,
    JpaAnswerRepository.class,
    JpaSubmissionRepository.class,
    JpaChallengeAttemptReader.class,
    JpaQuestionProgressReader.class
})
class JpaEvaluationRepositoriesTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaTagRepository tags;
    @Autowired private JpaQuestionRepository questions;
    @Autowired private JpaChallengeRepository challenges;
    @Autowired private JpaUserRepository users;
    @Autowired private JpaAnswerRepository answers;
    @Autowired private JpaSubmissionRepository submissions;
    @Autowired private JpaChallengeAttemptReader attempts;
    @Autowired private JpaQuestionProgressReader progress;
    @Autowired private TestEntityManager entityManager;

    private record Fixture(Challenge soma, Challenge outro, Challenge erro, Question question) {}

    @Test
    void keepsASubmissionThroughItsLifeWithTheResultsInPositionOrder() {
        Fixture f = fixture();
        UUID ana = user("vida");
        CodeSubmission queued =
                submissions.save(
                        CodeSubmission.queue(
                                UUID.randomUUID(),
                                ana,
                                f.soma().id(),
                                Language.PYTHON,
                                "class Solution:\n    pass  # ação\n",
                                SubmissionKind.SUBMIT,
                                4,
                                40,
                                T0));
        CodeSubmission running = submissions.save(queued.start());
        Verdict verdict =
                new Verdict(
                        SubmissionStatus.FAILED,
                        List.of(
                                new CaseResult(3, false, CaseStatus.TIME_LIMIT, null, null, null),
                                new CaseResult(0, true, CaseStatus.PASSED, "[0,1]", null, 2),
                                new CaseResult(1, true, CaseStatus.FAILED, "\"ação\"", null, 1),
                                new CaseResult(
                                        2,
                                        false,
                                        CaseStatus.ERROR,
                                        null,
                                        "ZeroDivisionError: division by zero",
                                        0)),
                        null,
                        null);
        submissions.save(running.finish(verdict, false, T0.plusSeconds(3)));
        entityManager.clear();

        CodeSubmission read = submissions.findById(queued.id()).orElseThrow();

        assertThat(read.status()).isEqualTo(SubmissionStatus.FAILED);
        assertThat(read.sourceCode()).isEqualTo("class Solution:\n    pass  # ação\n");
        assertThat(read.passedCount()).isEqualTo(1);
        assertThat(read.totalCount()).isEqualTo(4);
        assertThat(read.xpReward()).isEqualTo(40);
        assertThat(read.createdAt()).isEqualTo(T0);
        assertThat(read.finishedAt()).isEqualTo(T0.plusSeconds(3));
        assertThat(read.results()).extracting(CaseResult::position).containsExactly(0, 1, 2, 3);
        assertThat(read.results().get(1).actualJson()).isEqualTo("\"ação\"");
        assertThat(read.results().get(2).errorMessage())
                .isEqualTo("ZeroDivisionError: division by zero");
        assertThat(read.results().get(3).timeMs()).isNull();
        assertThat(submissions.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void listsTheSubmissionsOfAPersonInAChallengeNewestFirstByKind() {
        Fixture f = fixture();
        UUID ana = user("lista");
        CodeSubmission first =
                submissions.save(
                        finished(
                                new UUID(0, 2),
                                ana,
                                f.soma(),
                                SubmissionKind.SUBMIT,
                                SubmissionStatus.FAILED,
                                0,
                                T0));
        CodeSubmission second =
                submissions.save(
                        finished(
                                new UUID(0, 1),
                                ana,
                                f.soma(),
                                SubmissionKind.SUBMIT,
                                SubmissionStatus.PASSED,
                                40,
                                T0.plusSeconds(60)));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.soma(),
                        SubmissionKind.RUN,
                        SubmissionStatus.PASSED,
                        0,
                        T0.plusSeconds(90)));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.outro(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.FAILED,
                        0,
                        T0));
        entityManager.clear();

        assertThat(submissions.findByUserAndChallenge(ana, f.soma().id(), SubmissionKind.SUBMIT))
                .extracting(CodeSubmission::id)
                .containsExactly(second.id(), first.id());
        assertThat(submissions.findByUserAndChallenge(ana, f.soma().id(), SubmissionKind.RUN))
                .hasSize(1);
        assertThat(submissions.hasXpGained(ana, f.soma().id())).isTrue();
        assertThat(submissions.hasXpGained(ana, f.outro().id())).isFalse();
    }

    @Test
    void theAttemptReaderReadsOnlyTheSubmitsOldestFirst() {
        Fixture f = fixture();
        UUID ana = user("tentativas");
        UUID bia = user("outra-pessoa");
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.soma(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.PASSED,
                        40,
                        T0.plusSeconds(20)));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.soma(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.FAILED,
                        0,
                        T0.plusSeconds(10)));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.soma(),
                        SubmissionKind.RUN,
                        SubmissionStatus.PASSED,
                        0,
                        T0.plusSeconds(5)));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.outro(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.QUEUED,
                        0,
                        T0.plusSeconds(30)));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        bia,
                        f.soma(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.FAILED,
                        0,
                        T0));
        entityManager.clear();

        assertThat(attempts.findByUser(ana))
                .extracting(ChallengeAttempt::status)
                .containsExactly(
                        SubmissionStatus.FAILED, SubmissionStatus.PASSED, SubmissionStatus.QUEUED);
        assertThat(attempts.findByUser(ana).get(1).xpGained()).isEqualTo(40);
        assertThat(attempts.findByChallenges(List.of(f.soma().id())))
                .extracting(ChallengeAttempt::userId)
                .containsExactly(bia, ana, ana);
        assertThat(attempts.findByChallenges(List.of())).isEmpty();
    }

    @Test
    void theTrackReadsChallengesBySlugNextToTheQuestions() {
        Fixture f = fixture();
        UUID ana = user("trilha");
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.soma(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.FAILED,
                        0,
                        T0));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.soma(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.PASSED,
                        40,
                        T0.plusSeconds(1)));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.outro(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.COMPILE_ERROR,
                        0,
                        T0));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.erro(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.ERROR,
                        0,
                        T0));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.erro(),
                        SubmissionKind.RUN,
                        SubmissionStatus.PASSED,
                        0,
                        T0));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.erro(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.RUNNING,
                        0,
                        T0));
        QuestionOption wrong = f.question().options().get(0);
        answers.save(
                new Answer(UUID.randomUUID(), ana, f.question().id(), wrong.id(), false, 0, T0));
        entityManager.clear();

        Map<String, QuestionProgress> read = progress.answeredBy(ana);

        assertThat(read)
                .containsOnly(
                        entry(f.soma().slug(), QuestionProgress.SOLVED),
                        entry(f.outro().slug(), QuestionProgress.ATTEMPTED),
                        entry(f.question().slug(), QuestionProgress.ATTEMPTED));
    }

    @Test
    void onlyOneSubmissionPerPersonAndChallengeEarnsXp() {
        Fixture f = fixture();
        UUID ana = user("xp-desafio");
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.soma(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.PASSED,
                        40,
                        T0));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.soma(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.PASSED,
                        0,
                        T0.plusSeconds(1)));
        submissions.save(
                finished(
                        UUID.randomUUID(),
                        ana,
                        f.outro(),
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.PASSED,
                        40,
                        T0));

        assertThatThrownBy(
                        () ->
                                submissions.save(
                                        finished(
                                                UUID.randomUUID(),
                                                ana,
                                                f.soma(),
                                                SubmissionKind.SUBMIT,
                                                SubmissionStatus.PASSED,
                                                40,
                                                T0.plusSeconds(2))))
                .isInstanceOf(XpAlreadyGrantedException.class);
    }

    @Test
    void aSubmissionPointsToAUserThatExists() {
        Fixture f = fixture();

        assertThatThrownBy(
                        () ->
                                submissions.save(
                                        finished(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                f.soma(),
                                                SubmissionKind.SUBMIT,
                                                SubmissionStatus.FAILED,
                                                0,
                                                T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void expiringAfterTheWorkerFinishedChangesNothing() {
        Fixture f = fixture();
        CodeSubmission running = runningSubmission(f, user("expira-depois"));
        Verdict passed = new Verdict(SubmissionStatus.PASSED, List.of(), null, null);
        submissions.save(running.finish(passed, true, T0.plusSeconds(3)));
        entityManager.clear();

        submissions.save(running.expireAt(T0.plusSeconds(200)));
        entityManager.clear();

        CodeSubmission read = submissions.findById(running.id()).orElseThrow();
        assertThat(read.status()).isEqualTo(SubmissionStatus.PASSED);
        assertThat(read.xpGained()).isEqualTo(40);
        assertThat(read.finishedAt()).isEqualTo(T0.plusSeconds(3));
    }

    @Test
    void finishingAfterTheReaderExpiredItChangesNothing() {
        Fixture f = fixture();
        CodeSubmission running = runningSubmission(f, user("termina-depois"));
        submissions.save(running.expireAt(T0.plusSeconds(200)));
        entityManager.clear();

        Verdict passed = new Verdict(SubmissionStatus.PASSED, List.of(), null, null);
        submissions.save(running.finish(passed, true, T0.plusSeconds(210)));
        entityManager.clear();

        CodeSubmission read = submissions.findById(running.id()).orElseThrow();
        assertThat(read.status()).isEqualTo(SubmissionStatus.ERROR);
        assertThat(read.xpGained()).isZero();
        assertThat(submissions.hasXpGained(running.userId(), f.soma().id())).isFalse();
    }

    private CodeSubmission runningSubmission(Fixture f, UUID user) {
        CodeSubmission queued =
                CodeSubmission.queue(
                        UUID.randomUUID(),
                        user,
                        f.soma().id(),
                        Language.JAVA,
                        "class Solution {}",
                        SubmissionKind.SUBMIT,
                        4,
                        40,
                        T0);
        return submissions.save(queued.start());
    }

    private static CodeSubmission finished(
            UUID id,
            UUID user,
            Challenge challenge,
            SubmissionKind kind,
            SubmissionStatus status,
            int xpGained,
            Instant at) {
        boolean run = kind == SubmissionKind.RUN;
        return new CodeSubmission(
                id,
                user,
                challenge.id(),
                Language.JAVA,
                "class Solution {}",
                kind,
                status,
                0,
                run ? 2 : 4,
                null,
                null,
                run ? 0 : 40,
                xpGained,
                at,
                status.finished() ? at : null,
                List.of(),
                null);
    }

    private Fixture fixture() {
        Area area =
                areas.save(
                        new Area(
                                UUID.randomUUID(),
                                "zz-envios",
                                "Área de envios",
                                new SessionConfig(8, 2, 45, 0.5),
                                true));
        tags.save(new Tag(UUID.randomUUID(), "zz-arrays", "Arrays"));
        Question question =
                questions.save(
                        new Question(
                                UUID.randomUUID(),
                                "zz-q-envios",
                                "Questão",
                                area.id(),
                                Level.JUNIOR,
                                Difficulty.EASY,
                                "Enunciado",
                                "Explicação",
                                List.of(
                                        new QuestionOption(UUID.randomUUID(), "A", false, 0),
                                        new QuestionOption(UUID.randomUUID(), "B", true, 1)),
                                Set.of("zz-arrays"),
                                true));
        return new Fixture(
                challenges.save(challenge("zz-soma", area)),
                challenges.save(challenge("zz-outro", area)),
                challenges.save(challenge("zz-erro", area)),
                question);
    }

    private UUID user(String name) {
        UUID id = UUID.randomUUID();
        users.save(User.register(id, name + "@example.com", name, "hash", T0));
        return id;
    }

    private static Challenge challenge(String slug, Area area) {
        return new Challenge(
                UUID.randomUUID(),
                slug,
                "Desafio " + slug,
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
                        new ChallengeLanguage(Language.JAVA, "f", "class Solution {}", "ref")),
                List.of(
                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                Set.of("zz-arrays"),
                true);
    }
}
