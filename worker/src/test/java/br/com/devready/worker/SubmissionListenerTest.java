package br.com.devready.worker;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.catalog.port.TagRepository;
import br.com.devready.application.evaluation.port.CodeExecutor;
import br.com.devready.application.evaluation.port.SubmissionQueue;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.evaluation.usecase.ProcessSubmissionUseCase;
import br.com.devready.application.gamification.port.AchievementRepository;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.shared.Sleeper;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.ChallengeLanguage;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.ExecutionLimits;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.catalog.Tag;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import br.com.devready.infrastructure.evaluation.messaging.EvaluationMessaging;
import br.com.devready.worker.evaluation.SubmissionListener;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
@Import({TestcontainersConfiguration.class, SubmissionListenerTest.NoWait.class})
@ActiveProfiles("test")
class SubmissionListenerTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);
    private static final WireMockServer JUDGE0 = startJudge0();
    private static final AtomicBoolean TRANSACTION_SEEN = new AtomicBoolean();

    @TestConfiguration(proxyBeanMethods = false)
    static class NoWait {
        @Bean
        @Primary
        Sleeper noWait() {
            return duration -> {};
        }

        @Bean
        static BeanPostProcessor transactionSpy() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String name) {
                    if (bean instanceof CodeExecutor executor) {
                        return (CodeExecutor)
                                request -> {
                                    if (TransactionSynchronizationManager
                                            .isActualTransactionActive()) {
                                        TRANSACTION_SEEN.set(true);
                                    }
                                    return executor.execute(request);
                                };
                    }
                    return bean;
                }
            };
        }
    }

    @Autowired private SubmissionRepository submissions;
    @Autowired private SubmissionQueue queue;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private AreaRepository areas;
    @Autowired private TagRepository tags;
    @Autowired private ChallengeRepository challenges;
    @Autowired private UserRepository users;
    @Autowired private SubmissionListener listener;
    @Autowired private ProcessSubmissionUseCase processSubmission;

    @Autowired private SessionRepository sessions;
    @Autowired private AchievementRepository achievements;

    private Challenge challenge;
    private UUID user;

    private static WireMockServer startJudge0() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        return server;
    }

    @DynamicPropertySource
    static void judge0(DynamicPropertyRegistry registry) {
        registry.add("devready.judge0.url", JUDGE0::baseUrl);
    }

    @AfterAll
    static void stopJudge0() {
        JUDGE0.stop();
    }

    @BeforeEach
    void fixture() {
        JUDGE0.resetAll();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Area area =
                areas.save(
                        new Area(
                                UUID.randomUUID(),
                                "zz-worker-" + suffix,
                                "Área do worker",
                                new SessionConfig(8, 2, 45, 0.5),
                                true));
        tags.save(new Tag(UUID.randomUUID(), "zz-worker-" + suffix, "Worker"));
        challenge =
                challenges.save(
                        new Challenge(
                                UUID.randomUUID(),
                                "zz-worker-" + suffix,
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
                                        new ChallengeLanguage(
                                                Language.JAVA, "f", "class Solution {}", "ref")),
                                List.of(
                                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                                Set.of("zz-worker-" + suffix),
                                true));
        user = UUID.randomUUID();
        users.save(User.register(user, "worker-" + suffix + "@example.com", "Worker", "hash", T0));
    }

    private CodeSubmission queued(String code) {
        return submissions.save(
                CodeSubmission.queue(
                        UUID.randomUUID(),
                        user,
                        challenge.id(),
                        Language.JAVA,
                        code,
                        SubmissionKind.SUBMIT,
                        4,
                        40,
                        T0));
    }

    private SubmissionStatus statusOf(CodeSubmission submission) {
        return submissions.findById(submission.id()).orElseThrow().status();
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void compileErrorFor(String token) {
        JUDGE0.stubFor(
                post(urlPathEqualTo("/submissions"))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"token\":\"" + token + "\"}")));
        JUDGE0.stubFor(
                get(urlPathEqualTo("/submissions/" + token))
                        .willReturn(
                                okJson(
                                        "{\"status\":{\"id\":6,\"description\":\"Compilation Error\"},"
                                                + "\"compile_output\":\""
                                                + base64("Main.java:1: error: reached end of file")
                                                + "\"}")));
    }

    @Test
    void aPublishedSubmissionIsExecutedInJudge0AndStored() {
        compileErrorFor("t-compila");
        CodeSubmission submission = queued("class Solution {");

        queue.publish(submission.id());

        await().atMost(Duration.ofSeconds(30))
                .until(() -> statusOf(submission) == SubmissionStatus.COMPILE_ERROR);
        assertThat(submissions.findById(submission.id()).orElseThrow().compileOutput())
                .isEqualTo("Main.java:1: error: reached end of file");
        JUDGE0.verify(
                postRequestedFor(urlPathEqualTo("/submissions"))
                        .withRequestBody(matchingJsonPath("$.language_id", equalTo("62")))
                        .withRequestBody(matchingJsonPath("$.memory_limit", equalTo("8388608"))));
    }

    @Test
    void aRepeatedMessageIsIgnored() {
        compileErrorFor("t-repetido");
        CodeSubmission submission = queued("class Solution {");
        queue.publish(submission.id());
        await().atMost(Duration.ofSeconds(30))
                .until(() -> statusOf(submission) == SubmissionStatus.COMPILE_ERROR);

        queue.publish(submission.id());

        await().during(Duration.ofSeconds(1))
                .atMost(Duration.ofSeconds(3))
                .until(
                        () ->
                                JUDGE0.findAll(postRequestedFor(urlPathEqualTo("/submissions")))
                                                .size()
                                        == 1);
    }

    @Test
    void whenJudge0IsDownTheSubmissionBecomesAnErrorAndTheIdGoesToTheDeadLetterQueue() {
        JUDGE0.stubFor(
                post(urlPathEqualTo("/submissions")).willReturn(aResponse().withStatus(500)));
        CodeSubmission submission = queued("class Solution {}");

        queue.publish(submission.id());

        await().atMost(Duration.ofSeconds(30))
                .until(() -> statusOf(submission) == SubmissionStatus.ERROR);
        assertThat(JUDGE0.findAll(postRequestedFor(urlPathEqualTo("/submissions")))).hasSize(4);
        assertThat(submissions.findById(submission.id()).orElseThrow().errorMessage())
                .isEqualTo("O executor de código não respondeu. Tente de novo em alguns minutos.");
        await().atMost(Duration.ofSeconds(10))
                .until(
                        () ->
                                submission
                                        .id()
                                        .toString()
                                        .equals(
                                                rabbit.receiveAndConvert(
                                                        EvaluationMessaging.DEAD_LETTER_QUEUE,
                                                        500)));
    }

    @Test
    void theFlowRunsWithoutATransactionAroundTheUseCase() {
        compileErrorFor("t-sem-transacao");
        TRANSACTION_SEEN.set(false);
        CodeSubmission submission = queued("class Solution {");

        queue.publish(submission.id());

        await().atMost(Duration.ofSeconds(30))
                .until(() -> statusOf(submission) == SubmissionStatus.COMPILE_ERROR);
        assertThat(AopUtils.isAopProxy(listener)).isFalse();
        assertThat(AopUtils.isAopProxy(processSubmission)).isFalse();
        assertThat(TRANSACTION_SEEN.get()).isFalse();
    }

    @Test
    void theLastSubmissionOfASubmittedSessionCorrectsItAndUnlocksTheFirstSimulation() {
        compileErrorFor("t-sessao");
        Session session =
                sessions.create(
                        Session.simulation(
                                UUID.randomUUID(),
                                user,
                                challenge.areaId(),
                                Level.JUNIOR,
                                new SessionConfig(0, 1, 45, 0.5),
                                List.of(),
                                List.of(challenge.id()),
                                Instant.now()));
        CodeSubmission submission =
                submissions.save(
                        CodeSubmission.queueInSession(
                                UUID.randomUUID(),
                                user,
                                challenge.id(),
                                Language.JAVA,
                                "class Solution {",
                                SubmissionKind.SUBMIT,
                                4,
                                session.id(),
                                Instant.now()));
        sessions.submit(session.id());

        queue.publish(submission.id());

        await().atMost(Duration.ofSeconds(30))
                .until(
                        () ->
                                sessions.findById(session.id()).orElseThrow().status()
                                        == SessionStatus.COMPLETED);
        Session corrected = sessions.findById(session.id()).orElseThrow();
        assertThat(corrected.challengeScore()).isZero();
        assertThat(corrected.xpAwarded()).isEqualTo(10);
        assertThat(corrected.challenges().get(0).bestSubmissionId()).isEqualTo(submission.id());
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () ->
                                assertThat(achievements.findByUser(user))
                                        .extracting(
                                                UnlockedAchievement::achievement,
                                                UnlockedAchievement::sessionId)
                                        .contains(
                                                Tuple.tuple(
                                                        Achievement.FIRST_SIMULATION,
                                                        session.id())));
    }
}
