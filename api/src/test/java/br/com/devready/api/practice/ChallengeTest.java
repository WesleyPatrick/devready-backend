package br.com.devready.api.practice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@TestPropertySource(properties = "devready.seed.enabled=true")
@Import(FakeEvaluation.class)
class ChallengeTest extends IntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String JUNIOR_JAVA =
            "{\"companies\":[\"amazon\",\"google\",\"nubank\",\"meta\"],"
                    + "\"stack\":\"java-spring\",\"seniority\":\"JUNIOR\"}";
    private static final String TWO_SUM = "two-sum";
    private static final String DAILY = "backend-junior-java-equals-hashcode";

    @Autowired private SubmissionRepository submissions;
    @Autowired private ChallengeRepository challenges;

    @Test
    void everyRouteNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/v1/challenges/" + TWO_SUM)).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/v1/challenges/" + TWO_SUM + "/runs")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/v1/challenges/" + TWO_SUM + "/submissions")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/challenges/" + TWO_SUM + "/submissions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/submissions/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theChallengeComesWithItsLanguagesAndVisibleCasesButNeverTheReference() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        String body =
                body(
                        challenge(access, TWO_SUM)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.slug").value(TWO_SUM))
                                .andExpect(jsonPath("$.title").value("Soma de dois números"))
                                .andExpect(jsonPath("$.difficulty").value("EASY"))
                                .andExpect(
                                        jsonPath("$.competency.slug")
                                                .value("estruturas-de-dados-e-algoritmos"))
                                .andExpect(jsonPath("$.askedBy[0]").value("amazon"))
                                .andExpect(jsonPath("$.status").value("NOT_STARTED"))
                                .andExpect(jsonPath("$.xp").value(40))
                                .andExpect(
                                        jsonPath(
                                                "$.statement",
                                                startsWith("Dado um array de inteiros")))
                                .andExpect(jsonPath("$.languages.length()").value(3))
                                .andExpect(jsonPath("$.languages[0].language").value("JAVA"))
                                .andExpect(jsonPath("$.languages[0].runtime").value("Java 13"))
                                .andExpect(jsonPath("$.languages[0].functionName").value("twoSum"))
                                .andExpect(jsonPath("$.languages[1].runtime").value("Python 3.8"))
                                .andExpect(jsonPath("$.languages[1].functionName").value("two_sum"))
                                .andExpect(jsonPath("$.languages[2].runtime").value("Node 12"))
                                .andExpect(jsonPath("$.visibleCases.length()").value(2))
                                .andExpect(jsonPath("$.visibleCases[0].position").value(0))
                                .andExpect(jsonPath("$.visibleCases[0].args[1]").value(9))
                                .andExpect(jsonPath("$.visibleCases[0].expected[1]").value(1))
                                .andExpect(jsonPath("$.hiddenCaseCount").value(4))
                                .andExpect(jsonPath("$.limits.timeLimitMs").value(5000))
                                .andExpect(jsonPath("$.limits.memoryLimitKb").value(262144))
                                .andExpect(jsonPath("$.approachSummary").value(nullValue())));

        assertThat(body).doesNotContain("referenceSolution").doesNotContain("seen.put(nums[i], i)");
        challenge(register(), TWO_SUM)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.askedBy.length()").value(0));
    }

    @Test
    void runningAndSubmittingChangeTheStatusTheXpTheListTheProgressAndTheTrack() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);
        String starter =
                JsonPath.read(body(challenge(access, TWO_SUM)), "$.languages[0].starterCode");

        String run = finished(access, ticket(run(access, TWO_SUM, "JAVA", starter)));
        assertThat(JsonPath.<String>read(run, "$.kind")).isEqualTo("RUN");
        assertThat(JsonPath.<Boolean>read(run, "$.sealed")).isFalse();
        assertThat(JsonPath.<String>read(run, "$.status")).isEqualTo("FAILED");
        assertThat(JsonPath.<Integer>read(run, "$.totalCount")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(run, "$.passedCount")).isZero();
        assertThat(JsonPath.<String>read(run, "$.sourceCode")).isEqualTo(starter);
        assertThat(JsonPath.<String>read(run, "$.challengeSlug")).isEqualTo(TWO_SUM);
        assertThat(JsonPath.<List<Object>>read(run, "$.cases[1].args[0]")).containsExactly(3, 2, 4);
        assertThat(JsonPath.<List<Object>>read(run, "$.cases[1].expected")).containsExactly(1, 2);
        assertThat(JsonPath.<List<Object>>read(run, "$.cases[1].actual")).isEmpty();
        submissions(access, TWO_SUM).andExpect(jsonPath("$.length()").value(0));
        challenge(access, TWO_SUM).andExpect(jsonPath("$.status").value("NOT_STARTED"));
        progress(access).andExpect(jsonPath("$.dailyGoal.done").value(0));

        String wrong = finished(access, ticket(submit(access, TWO_SUM, "JAVA", starter)));
        assertThat(JsonPath.<String>read(wrong, "$.status")).isEqualTo("FAILED");
        assertThat(JsonPath.<Integer>read(wrong, "$.totalCount")).isEqualTo(6);
        assertThat(JsonPath.<Integer>read(wrong, "$.xpGained")).isZero();
        Map<String, Object> hidden = JsonPath.read(wrong, "$.cases[2]");
        assertThat(hidden)
                .containsEntry("visible", false)
                .containsKeys("position", "status", "timeMs");
        assertThat(hidden).doesNotContainKeys("args", "expected", "actual", "error");
        challenge(access, TWO_SUM).andExpect(jsonPath("$.status").value("ATTEMPTED"));
        list(access, "type=CHALLENGE")
                .andExpect(
                        jsonPath("$.items[?(@.slug == 'two-sum')].status")
                                .value(hasItem("ATTEMPTED")));
        list(access, "type=CHALLENGE")
                .andExpect(jsonPath("$.items[?(@.slug == 'two-sum')].inExam", hasItem(false)));
        progress(access)
                .andExpect(jsonPath("$.xp").value(0))
                .andExpect(jsonPath("$.dailyGoal.done").value(1));

        String right =
                finished(
                        access,
                        ticket(
                                submit(
                                        access,
                                        TWO_SUM,
                                        "JAVA",
                                        "// " + FakeEvaluation.SOLVES + "\n" + starter)));
        assertThat(JsonPath.<String>read(right, "$.status")).isEqualTo("PASSED");
        assertThat(JsonPath.<Integer>read(right, "$.passedCount")).isEqualTo(6);
        assertThat(JsonPath.<Integer>read(right, "$.xpGained")).isEqualTo(40);
        challenge(access, TWO_SUM)
                .andExpect(jsonPath("$.status").value("SOLVED"))
                .andExpect(jsonPath("$.approachSummary", startsWith("A solução com dois laços")));
        progress(access)
                .andExpect(jsonPath("$.xp").value(40))
                .andExpect(jsonPath("$.dailyGoal.done").value(2));

        String again =
                finished(
                        access,
                        ticket(submit(access, TWO_SUM, "PYTHON", "# " + FakeEvaluation.SOLVES)));
        assertThat(JsonPath.<Integer>read(again, "$.xpGained")).isZero();
        progress(access).andExpect(jsonPath("$.xp").value(40));
        submissions(access, TWO_SUM)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].status").value("PASSED"))
                .andExpect(jsonPath("$[0].language").value("PYTHON"))
                .andExpect(jsonPath("$[0].passedCount").value(6))
                .andExpect(jsonPath("$[0].createdAt").isString())
                .andExpect(jsonPath("$[0].sourceCode").doesNotExist())
                .andExpect(jsonPath("$[2].status").value("FAILED"));

        List<String> options =
                JsonPath.read(
                        body(
                                mockMvc.perform(
                                        get("/api/v1/questions/" + DAILY)
                                                .header("Authorization", "Bearer " + access))),
                        "$.options[*].id");
        mockMvc.perform(
                        post("/api/v1/questions/" + DAILY + "/answer")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content("{\"optionId\":\"" + options.get(2) + "\"}"))
                .andExpect(jsonPath("$.correct").value(true));
        track(access)
                .andExpect(
                        jsonPath("$.competencies[0].slug")
                                .value("estruturas-de-dados-e-algoritmos"))
                .andExpect(jsonPath("$.competencies[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.plan.doneCompetencies").value(0));
        competency(access, "estruturas-de-dados-e-algoritmos")
                .andExpect(
                        jsonPath("$.challenges[?(@.slug == 'two-sum')].status")
                                .value(hasItem("SOLVED")))
                .andExpect(
                        jsonPath("$.challenges[?(@.slug == 'valid-parentheses')].status")
                                .value(hasItem("NOT_STARTED")));
    }

    @Test
    void aCompileErrorComesBackWithTheCompilerOutputAndCountsAsAnAttempt() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        String broken =
                finished(
                        access,
                        ticket(
                                submit(
                                        access,
                                        TWO_SUM,
                                        "PYTHON",
                                        "# " + FakeEvaluation.BREAKS_THE_BUILD)));

        assertThat(JsonPath.<String>read(broken, "$.status")).isEqualTo("COMPILE_ERROR");
        assertThat(JsonPath.<String>read(broken, "$.compileOutput"))
                .isEqualTo("Main.java:3: error: ';' expected");
        assertThat(JsonPath.<String>read(broken, "$.language")).isEqualTo("PYTHON");
        assertThat(JsonPath.<List<Object>>read(broken, "$.cases")).isEmpty();
        challenge(access, TWO_SUM).andExpect(jsonPath("$.status").value("ATTEMPTED"));
    }

    @Test
    void errors() throws Exception {
        String access = register();

        challenge(access, "nao-existe")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Desafio não encontrado: nao-existe."));
        challenge(access, "backend-junior-sql-index").andExpect(status().isNotFound());
        run(access, "nao-existe", "JAVA", "x").andExpect(status().isNotFound());
        mockMvc.perform(
                        post("/api/v1/challenges/" + TWO_SUM + "/runs")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(
                        jsonPath("$.errors[?(@.field == 'language')].message")
                                .value(hasItem("Escolha a linguagem.")))
                .andExpect(
                        jsonPath("$.errors[?(@.field == 'sourceCode')].message")
                                .value(hasItem("Escreva o código antes de rodar ou enviar.")));
        mockMvc.perform(
                        post("/api/v1/challenges/" + TWO_SUM + "/submissions")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content("{\"language\":\"RUBY\",\"sourceCode\":\"x\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("language"));
        submit(access, TWO_SUM, "JAVA", "   ")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("sourceCode"));
        submit(access, TWO_SUM, "JAVA", "x".repeat(CodeSubmission.MAX_SOURCE_CHARS + 1))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].message").value("O código passa de 64 KB."));
        submission(access, UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Envio não encontrado."));
        String someoneElses = ticket(submit(register(), TWO_SUM, "JAVA", "class Solution {}"));
        submission(access, someoneElses).andExpect(status().isNotFound());
        submission(access, "nao-e-um-id").andExpect(status().isBadRequest());
    }

    @Test
    void aSubmissionStuckForMoreThanTwoMinutesBecomesAnErrorOnTheNextRead() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);
        UUID user =
                UUID.fromString(
                        JsonPath.read(
                                body(
                                        mockMvc.perform(
                                                get("/api/v1/auth/me")
                                                        .header(
                                                                "Authorization",
                                                                "Bearer " + access))),
                                "$.id"));
        Challenge twoSum = challenges.findBySlug(TWO_SUM).orElseThrow();
        CodeSubmission stuck =
                submissions.save(
                        CodeSubmission.queue(
                                UUID.randomUUID(),
                                user,
                                twoSum.id(),
                                Language.JAVA,
                                "class Solution {}",
                                SubmissionKind.SUBMIT,
                                6,
                                40,
                                Instant.now().minus(Duration.ofMinutes(3))));

        submission(access, stuck.id().toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(
                        jsonPath("$.errorMessage")
                                .value("O envio não terminou a tempo. Tente de novo."))
                .andExpect(jsonPath("$.finishedAt").isString());
    }

    private String finished(String access, String id) {
        AtomicReference<String> last = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(
                        () -> {
                            String current =
                                    body(submission(access, id).andExpect(status().isOk()));
                            assertThat(JsonPath.<String>read(current, "$.status"))
                                    .isNotIn("QUEUED", "RUNNING");
                            last.set(current);
                        });
        return last.get();
    }

    private static String ticket(ResultActions actions) throws Exception {
        String response =
                body(
                        actions.andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.status").value("QUEUED")));
        return JsonPath.read(response, "$.id");
    }

    private ResultActions run(String access, String slug, String language, String code)
            throws Exception {
        return code(access, "/api/v1/challenges/" + slug + "/runs", language, code);
    }

    private ResultActions submit(String access, String slug, String language, String code)
            throws Exception {
        return code(access, "/api/v1/challenges/" + slug + "/submissions", language, code);
    }

    private ResultActions code(String access, String path, String language, String code)
            throws Exception {
        return mockMvc.perform(
                post(path)
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content(
                                JSON.writeValueAsString(
                                        Map.of("language", language, "sourceCode", code))));
    }

    private ResultActions challenge(String access, String slug) throws Exception {
        return mockMvc.perform(
                get("/api/v1/challenges/" + slug).header("Authorization", "Bearer " + access));
    }

    private ResultActions submissions(String access, String slug) throws Exception {
        return mockMvc.perform(
                get("/api/v1/challenges/" + slug + "/submissions")
                        .header("Authorization", "Bearer " + access));
    }

    private ResultActions submission(String access, String id) throws Exception {
        return mockMvc.perform(
                get("/api/v1/submissions/" + id).header("Authorization", "Bearer " + access));
    }

    private ResultActions list(String access, String query) throws Exception {
        return mockMvc.perform(
                get("/api/v1/questions?" + query).header("Authorization", "Bearer " + access));
    }

    private ResultActions progress(String access) throws Exception {
        return mockMvc.perform(
                get("/api/v1/me/progress").header("Authorization", "Bearer " + access));
    }

    private ResultActions track(String access) throws Exception {
        return mockMvc.perform(
                get("/api/v1/career/track").header("Authorization", "Bearer " + access));
    }

    private ResultActions competency(String access, String slug) throws Exception {
        return mockMvc.perform(
                get("/api/v1/career/track/competencies/" + slug)
                        .header("Authorization", "Bearer " + access));
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    private String registerWithProfile(String profile) throws Exception {
        String access = register();
        mockMvc.perform(
                        put("/api/v1/career/profile")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content(profile))
                .andExpect(status().isOk());
        return access;
    }

    private String register() throws Exception {
        String response =
                mockMvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"d-"
                                                        + UUID.randomUUID()
                                                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
