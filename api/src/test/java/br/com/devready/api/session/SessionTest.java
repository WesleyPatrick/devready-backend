package br.com.devready.api.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.api.practice.FakeEvaluation;
import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.ArrayList;
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
class SessionTest extends IntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String JUNIOR_JAVA =
            "{\"companies\":[\"amazon\",\"google\",\"nubank\",\"meta\"],"
                    + "\"stack\":\"java-spring\",\"seniority\":\"JUNIOR\"}";
    private static final String SQL = "sql-e-modelagem-de-dados";

    @Autowired private QuestionRepository questions;
    @Autowired private ChallengeRepository challenges;

    @Test
    void everyRouteNeedsAToken() throws Exception {
        String id = UUID.randomUUID().toString();
        mockMvc.perform(
                        post("/api/v1/sessions/lessons")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/v1/sessions/simulations")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/sessions/current")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/sessions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/sessions/" + id)).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        put("/api/v1/sessions/" + id + "/questions/1")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/v1/sessions/" + id + "/challenges/1/runs")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/v1/sessions/" + id + "/challenges/1/submissions")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/sessions/" + id + "/finish"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/sessions/" + id + "/result"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/achievements")).andExpect(status().isUnauthorized());
    }

    @Test
    void aWholeSimulationFromTheStartToTheResult() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        String created =
                body(
                        startSimulation(access, "backend", "JUNIOR")
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.format").value("SIMULATION"))
                                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                                .andExpect(jsonPath("$.area.slug").value("backend"))
                                .andExpect(jsonPath("$.area.name").value("Backend"))
                                .andExpect(jsonPath("$.level").value("JUNIOR"))
                                .andExpect(jsonPath("$.competency").value(nullValue()))
                                .andExpect(jsonPath("$.deadlineAt").isString())
                                .andExpect(jsonPath("$.questions.length()").value(8))
                                .andExpect(jsonPath("$.questions[0].position").value(1))
                                .andExpect(
                                        jsonPath("$.questions[0].chosenOptionId")
                                                .value(nullValue()))
                                .andExpect(jsonPath("$.questions[0].correct").doesNotExist())
                                .andExpect(
                                        jsonPath("$.questions[0].correctOptionId").doesNotExist())
                                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist())
                                .andExpect(jsonPath("$.challenges.length()").value(2))
                                .andExpect(jsonPath("$.challenges[1].position").value(2))
                                .andExpect(jsonPath("$.challenges[0].submissionsLeft").value(5))
                                .andExpect(
                                        jsonPath("$.challenges[0].languages[0].runtime")
                                                .value("Java 13"))
                                .andExpect(
                                        jsonPath("$.challenges[0].visibleCases.length()").value(2))
                                .andExpect(
                                        jsonPath("$.challenges[0].bestSubmissionId")
                                                .value(nullValue())));
        assertThat(created).doesNotContain("referenceSolution");
        String id = JsonPath.read(created, "$.id");

        int position = trackQuestionPosition(access, created);
        String slug = JsonPath.read(created, "$.questions[" + (position - 1) + "].slug");
        Question question = questions.findBySlug(slug).orElseThrow();
        UUID right = question.correctOption().id();
        UUID wrong =
                question.options().stream()
                        .map(QuestionOption::id)
                        .filter(option -> !option.equals(right))
                        .findFirst()
                        .orElseThrow();
        answer(access, id, position, wrong)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(position))
                .andExpect(jsonPath("$.chosenOptionId").value(wrong.toString()))
                .andExpect(jsonPath("$.correct").doesNotExist())
                .andExpect(jsonPath("$.xpGained").value(0))
                .andExpect(jsonPath("$.sessionStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.newAchievements.length()").value(0));
        answer(access, id, position, right).andExpect(status().isOk());
        current(access)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(
                        jsonPath("$.questions[" + (position - 1) + "].chosenOptionId")
                                .value(right.toString()));
        startSimulation(access, "backend", "JUNIOR")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Sessão em andamento"))
                .andExpect(jsonPath("$.detail").value("Você já tem uma simulação em andamento."))
                .andExpect(jsonPath("$.sessionId").value(id));

        String locked = JsonPath.read(created, "$.challenges[0].slug");
        mockMvc.perform(
                        get("/api/v1/challenges/" + locked)
                                .header("Authorization", "Bearer " + access))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Sessão em andamento"))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Este desafio está na sua simulação em andamento."))
                .andExpect(jsonPath("$.sessionId").value(id));
        mockMvc.perform(
                        post("/api/v1/challenges/" + locked + "/runs")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content("{\"language\":\"JAVA\",\"sourceCode\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.sessionId").value(id));
        mockMvc.perform(
                        post("/api/v1/challenges/" + locked + "/submissions")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content("{\"language\":\"JAVA\",\"sourceCode\":\"x\"}"))
                .andExpect(status().isConflict());
        String challengeList =
                body(
                        mockMvc.perform(
                                get("/api/v1/questions?type=CHALLENGE&size=50")
                                        .header("Authorization", "Bearer " + access)));
        List<String> inSession =
                List.of(
                        JsonPath.read(created, "$.challenges[0].slug"),
                        JsonPath.read(created, "$.challenges[1].slug"));
        for (String slugInList : JsonPath.<List<String>>read(challengeList, "$.items[*].slug")) {
            List<Boolean> flag =
                    JsonPath.read(
                            challengeList, "$.items[?(@.slug == '" + slugInList + "')].inExam");
            assertThat(flag).containsExactly(inSession.contains(slugInList));
            if (!inSession.contains(slugInList)) {
                mockMvc.perform(
                                get("/api/v1/challenges/" + slugInList)
                                        .header("Authorization", "Bearer " + access))
                        .andExpect(status().isOk());
            }
        }

        List<String> submits = new ArrayList<>();
        for (int challenge = 1; challenge <= 2; challenge++) {
            String run =
                    finished(access, ticket(code(access, id, challenge, "runs", "SOMA_CERTA")));
            assertThat(JsonPath.<String>read(run, "$.kind")).isEqualTo("RUN");
            assertThat(JsonPath.<Boolean>read(run, "$.sealed")).isFalse();
            assertThat(JsonPath.<List<Object>>read(run, "$.cases")).as(run).hasSize(2);
            String submit =
                    finished(
                            access,
                            ticket(code(access, id, challenge, "submissions", "SOMA_CERTA")));
            assertThat(JsonPath.<Boolean>read(submit, "$.sealed")).isTrue();
            assertThat(JsonPath.<List<Object>>read(submit, "$.cases")).isEmpty();
            assertThat(JsonPath.<Integer>read(submit, "$.totalCount")).isGreaterThan(2);
            submits.add(JsonPath.read(submit, "$.id"));
        }
        session(access, id)
                .andExpect(jsonPath("$.challenges[0].submissionsUsed").value(1))
                .andExpect(jsonPath("$.challenges[0].submissionsLeft").value(4))
                .andExpect(jsonPath("$.challenges[0].lastSubmissionId").value(submits.get(0)))
                .andExpect(jsonPath("$.challenges[0].bestSubmissionId").value(submits.get(0)));
        result(access, id)
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.detail").value("O resultado sai quando a correção terminar."));
        String firstChallenge = JsonPath.read(created, "$.challenges[0].slug");
        mockMvc.perform(
                        get("/api/v1/challenges/" + firstChallenge + "/submissions")
                                .header("Authorization", "Bearer " + access))
                .andExpect(status().isConflict());

        finish(access, id)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        finish(access, id)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(
                        get("/api/v1/challenges/" + firstChallenge + "/submissions")
                                .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        String result =
                body(
                        result(access, id)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("COMPLETED"))
                                .andExpect(jsonPath("$.objectiveScore").value(0.125))
                                .andExpect(jsonPath("$.questions.length()").value(8))
                                .andExpect(
                                        jsonPath("$.questions[" + (position - 1) + "].correct")
                                                .value(true))
                                .andExpect(
                                        jsonPath(
                                                        "$.questions["
                                                                + (position - 1)
                                                                + "].correctOptionId")
                                                .value(right.toString()))
                                .andExpect(jsonPath("$.challenges.length()").value(2))
                                .andExpect(
                                        jsonPath("$.challenges[0].bestSubmissionId")
                                                .value(submits.get(0)))
                                .andExpect(
                                        jsonPath(
                                                "$.newAchievements", hasItem("primeira-simulacao")))
                                .andExpect(jsonPath("$.competencies[0].text").isString()));
        double objective = ((Number) JsonPath.read(result, "$.objectiveScore")).doubleValue();
        double challengeScore = ((Number) JsonPath.read(result, "$.challengeScore")).doubleValue();
        double finalScore = ((Number) JsonPath.read(result, "$.finalScore")).doubleValue();
        int xp = JsonPath.read(result, "$.xpAwarded");
        assertThat(finalScore).isCloseTo(0.5 * objective + 0.5 * challengeScore, within(0.0001));
        assertThat(xp).isEqualTo(Math.max(10, (int) Math.round(100 * finalScore)));

        submission(access, submits.get(0))
                .andExpect(jsonPath("$.sealed").value(false))
                .andExpect(jsonPath("$.cases.length()").isNumber());
        progress(access)
                .andExpect(jsonPath("$.xp").value(xp))
                .andExpect(jsonPath("$.dailyGoal.done").value(1));
        question(access, slug).andExpect(jsonPath("$.status").value("SOLVED"));
        String competency = JsonPath.read(body(question(access, slug)), "$.competency.slug");
        mockMvc.perform(
                        get("/api/v1/career/track/competencies/" + competency)
                                .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.questions[?(@.slug == '" + slug + "')].status",
                                hasItem("SOLVED")));
        mockMvc.perform(get("/api/v1/me/achievements").header("Authorization", "Bearer " + access))
                .andExpect(
                        jsonPath(
                                "$[?(@.slug == 'primeira-simulacao')].unlockedAt",
                                hasItem(org.hamcrest.Matchers.notNullValue())));
        sessions(access)
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].format").value("SIMULATION"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].xpAwarded").value(xp))
                .andExpect(jsonPath("$[0].finishedAt").isString());
        current(access).andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/v1/challenges/" + locked)
                                .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
    }

    @Test
    void aSealedSubmissionNeverShowsCompilerOutputOrTheRuntimeError() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);
        String id = JsonPath.read(body(startSimulation(access, "backend", "JUNIOR")), "$.id");

        String build =
                finished(
                        access,
                        ticket(
                                code(
                                        access,
                                        id,
                                        1,
                                        "submissions",
                                        FakeEvaluation.BREAKS_THE_BUILD)));
        String crash =
                finished(
                        access, ticket(code(access, id, 2, "submissions", FakeEvaluation.CRASHES)));
        assertThat(JsonPath.<String>read(build, "$.status")).isEqualTo("COMPILE_ERROR");
        assertThat(JsonPath.<String>read(crash, "$.status")).isEqualTo("RUNTIME_ERROR");
        for (String sealed : List.of(build, crash)) {
            assertThat(JsonPath.<Boolean>read(sealed, "$.sealed")).isTrue();
            assertThat(JsonPath.<Map<String, Object>>read(sealed, "$"))
                    .containsEntry("compileOutput", null)
                    .containsEntry("errorMessage", null);
            assertThat(JsonPath.<List<Object>>read(sealed, "$.cases")).isEmpty();
            assertThat(sealed)
                    .doesNotContain(FakeEvaluation.CRASH_OUTPUT)
                    .doesNotContain("expected");
        }

        finish(access, id).andExpect(status().isAccepted());
        String openedBuild = body(submission(access, JsonPath.read(build, "$.id")));
        String openedCrash = body(submission(access, JsonPath.read(crash, "$.id")));
        assertThat(JsonPath.<Boolean>read(openedBuild, "$.sealed")).isFalse();
        assertThat(JsonPath.<String>read(openedBuild, "$.compileOutput")).contains("expected");
        assertThat(JsonPath.<String>read(openedCrash, "$.errorMessage"))
                .isEqualTo(FakeEvaluation.CRASH_OUTPUT);
    }

    @Test
    void theQuestionsOfTheOpenSimulationAreLockedInThePracticeUntilTheCorrection()
            throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);
        String created = body(startSimulation(access, "backend", "JUNIOR"));
        String id = JsonPath.read(created, "$.id");
        List<String> inSession = JsonPath.read(created, "$.questions[*].slug");
        String locked = inSession.get(0);
        UUID right = questions.findBySlug(locked).orElseThrow().correctOption().id();

        question(access, locked)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Sessão em andamento"))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Esta questão está na sua simulação em andamento."))
                .andExpect(jsonPath("$.sessionId").value(id));
        practiceAnswer(access, locked, right)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Sessão em andamento"))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Esta questão está na sua simulação em andamento."))
                .andExpect(jsonPath("$.sessionId").value(id))
                .andExpect(jsonPath("$.correctOptionId").doesNotExist());
        progress(access).andExpect(jsonPath("$.dailyGoal.done").value(0));

        String list =
                body(
                        mockMvc.perform(
                                get("/api/v1/questions?type=QUESTION&size=50")
                                        .header("Authorization", "Bearer " + access)));
        List<Boolean> marked = new ArrayList<>();
        for (String slugInList : JsonPath.<List<String>>read(list, "$.items[*].slug")) {
            List<Boolean> flag =
                    JsonPath.read(list, "$.items[?(@.slug == '" + slugInList + "')].inExam");
            assertThat(flag).as(slugInList).containsExactly(inSession.contains(slugInList));
            marked.addAll(flag);
        }
        assertThat(marked).contains(true, false);
        String free =
                JsonPath.<List<String>>read(list, "$.items[?(@.inExam == false)].slug").get(0);
        question(access, free).andExpect(status().isOk());

        answer(access, id, 1, right).andExpect(status().isOk());

        finish(access, id)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        question(access, locked).andExpect(status().isOk());
        practiceAnswer(access, locked, right)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));
    }

    @Test
    void aLessonRevealsEachAnswerCountsAsPracticeAndClosesOnTheLastOne() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        String lesson =
                body(
                        startLesson(access, SQL)
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.format").value("LESSON"))
                                .andExpect(jsonPath("$.competency.slug").value(SQL))
                                .andExpect(jsonPath("$.level").value("JUNIOR"))
                                .andExpect(jsonPath("$.deadlineAt").value(nullValue()))
                                .andExpect(jsonPath("$.questions.length()").value(4))
                                .andExpect(jsonPath("$.challenges.length()").value(0)));
        String id = JsonPath.read(lesson, "$.id");
        int xp = 0;
        for (int position = 1; position <= 4; position++) {
            String slug = JsonPath.read(lesson, "$.questions[" + (position - 1) + "].slug");
            UUID right = questions.findBySlug(slug).orElseThrow().correctOption().id();
            String answered =
                    body(
                            answer(access, id, position, right)
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.correct").value(true))
                                    .andExpect(
                                            jsonPath("$.correctOptionId").value(right.toString()))
                                    .andExpect(jsonPath("$.explanation").isString())
                                    .andExpect(
                                            jsonPath("$.sessionStatus")
                                                    .value(
                                                            position == 4
                                                                    ? "COMPLETED"
                                                                    : "IN_PROGRESS")));
            int gained = JsonPath.read(answered, "$.xpGained");
            assertThat(gained).isPositive();
            xp += gained;
            if (position == 1) {
                assertThat(JsonPath.<List<String>>read(answered, "$.newAchievements"))
                        .contains("primeira-questao");
                answer(access, id, 1, right)
                        .andExpect(status().isUnprocessableContent())
                        .andExpect(
                                jsonPath("$.detail")
                                        .value("Esta questão da lição já foi respondida."));
            }
        }

        result(access, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectiveScore").value(1.0))
                .andExpect(jsonPath("$.finalScore").value(nullValue()))
                .andExpect(jsonPath("$.challengeScore").value(nullValue()))
                .andExpect(jsonPath("$.xpAwarded").value(0))
                .andExpect(jsonPath("$.competencies[0].competency.slug").value(SQL))
                .andExpect(jsonPath("$.competencies[0].verdict").value("STRONG"));
        progress(access)
                .andExpect(jsonPath("$.xp").value(xp))
                .andExpect(jsonPath("$.dailyGoal.done").value(3));

        String simulation =
                JsonPath.read(body(startSimulation(access, "backend", "JUNIOR")), "$.id");
        startLesson(access, SQL)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.sessionId").value(simulation));
        startLesson(access, "react").andExpect(status().isNotFound());
    }

    @Test
    void aCompetencyWithFewerThanThreeQuestionsHasNoLesson() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);
        List<String> competencies = JsonPath.read(body(track(access)), "$.competencies[*].slug");
        String small = null;
        for (String competency : competencies) {
            String detail =
                    body(
                            mockMvc.perform(
                                    get("/api/v1/career/track/competencies/" + competency)
                                            .header("Authorization", "Bearer " + access)));
            if (JsonPath.<Integer>read(detail, "$.questions.length()") < 3) {
                small = competency;
                break;
            }
        }
        assertThat(small).as("uma competência da trilha com menos de 3 questões").isNotNull();

        startLesson(access, small)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"));
        current(access).andExpect(status().isNotFound());
    }

    @Test
    void errors() throws Exception {
        String noGoal = register();
        startSimulation(noGoal, "backend", "JUNIOR").andExpect(status().isNotFound());
        startLesson(noGoal, SQL).andExpect(status().isNotFound());

        String access = registerWithProfile(JUNIOR_JAVA);
        startSimulation(access, "mobile", "JUNIOR")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Área desconhecida: mobile."));
        mockMvc.perform(
                        post("/api/v1/sessions/simulations")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content("{\"area\":\"backend\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("level"));
        startSimulation(access, "backend", "MESTRE").andExpect(status().isUnprocessableContent());

        String id = JsonPath.read(body(startSimulation(access, "backend", "JUNIOR")), "$.id");
        answer(access, id, 99, UUID.randomUUID())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Não existe questão 99 nesta sessão."));
        answer(access, id, 1, UUID.randomUUID())
                .andExpect(status().isUnprocessableContent())
                .andExpect(
                        jsonPath("$.detail").value("A alternativa escolhida não é desta questão."));
        code(access, id, 3, "runs", "x").andExpect(status().isUnprocessableContent());
        session(register(), id).andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/api/v1/sessions/nao-e-um-id")
                                .header("Authorization", "Bearer " + access))
                .andExpect(status().isBadRequest());

        for (int i = 0; i < 5; i++) {
            code(access, id, 1, "submissions", "x" + i).andExpect(status().isAccepted());
        }
        code(access, id, 1, "submissions", "y")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Os 5 envios do desafio 1 já foram usados."));

        finish(access, id).andExpect(status().isAccepted());
        answer(access, id, 1, UUID.randomUUID())
                .andExpect(status().isUnprocessableContent())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Esta sessão não aceita mais respostas nem envios."));
    }

    @Test
    void everyAreaStartsAPlenoAndASeniorSimulationWithItemsOfThatLevelOnly() throws Exception {
        Map<String, List<Integer>> sizes =
                Map.of(
                        "backend", List.of(8, 2),
                        "frontend", List.of(8, 2),
                        "devops", List.of(10, 1));
        for (Map.Entry<String, List<Integer>> area : sizes.entrySet()) {
            for (Level level : List.of(Level.PLENO, Level.SENIOR)) {
                String access = registerWithProfile(JUNIOR_JAVA);
                String session =
                        body(
                                startSimulation(access, area.getKey(), level.name())
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.level").value(level.name()))
                                        .andExpect(
                                                jsonPath("$.questions.length()")
                                                        .value(area.getValue().get(0)))
                                        .andExpect(
                                                jsonPath("$.challenges.length()")
                                                        .value(area.getValue().get(1))));
                for (String slug : JsonPath.<List<String>>read(session, "$.questions[*].slug")) {
                    assertThat(questions.findBySlug(slug).orElseThrow().level())
                            .as(area.getKey() + " " + slug)
                            .isEqualTo(level);
                }
                for (String slug : JsonPath.<List<String>>read(session, "$.challenges[*].slug")) {
                    assertThat(challenges.findBySlug(slug).orElseThrow().level())
                            .as(area.getKey() + " " + slug)
                            .isEqualTo(level);
                }
            }
        }
    }

    private int trackQuestionPosition(String access, String session) throws Exception {
        for (int position = 1; position <= 8; position++) {
            String slug = JsonPath.read(session, "$.questions[" + (position - 1) + "].slug");
            List<String> askedBy =
                    JsonPath.read(
                            body(
                                    mockMvc.perform(
                                            get("/api/v1/questions?size=50")
                                                    .header("Authorization", "Bearer " + access))),
                            "$.items[?(@.slug == '" + slug + "')].askedBy[*]");
            if (!askedBy.isEmpty()) {
                return position;
            }
        }
        throw new AssertionError("Nenhuma questão da simulação está na trilha.");
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
        return JsonPath.read(
                body(
                        actions.andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.status").value("QUEUED"))),
                "$.id");
    }

    private ResultActions startSimulation(String access, String area, String level)
            throws Exception {
        return mockMvc.perform(
                post("/api/v1/sessions/simulations")
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("area", area, "level", level))));
    }

    private ResultActions startLesson(String access, String competency) throws Exception {
        return mockMvc.perform(
                post("/api/v1/sessions/lessons")
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("competency", competency))));
    }

    private ResultActions answer(String access, String id, int position, UUID option)
            throws Exception {
        return mockMvc.perform(
                put("/api/v1/sessions/" + id + "/questions/" + position)
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content("{\"optionId\":\"" + option + "\"}"));
    }

    private ResultActions practiceAnswer(String access, String slug, UUID option) throws Exception {
        return mockMvc.perform(
                post("/api/v1/questions/" + slug + "/answer")
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content("{\"optionId\":\"" + option + "\"}"));
    }

    private ResultActions code(String access, String id, int position, String kind, String code)
            throws Exception {
        return mockMvc.perform(
                post("/api/v1/sessions/" + id + "/challenges/" + position + "/" + kind)
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content(
                                JSON.writeValueAsString(
                                        Map.of("language", "JAVA", "sourceCode", code))));
    }

    private ResultActions finish(String access, String id) throws Exception {
        return mockMvc.perform(
                post("/api/v1/sessions/" + id + "/finish")
                        .header("Authorization", "Bearer " + access));
    }

    private ResultActions session(String access, String id) throws Exception {
        return mockMvc.perform(
                get("/api/v1/sessions/" + id).header("Authorization", "Bearer " + access));
    }

    private ResultActions current(String access) throws Exception {
        return mockMvc.perform(
                get("/api/v1/sessions/current").header("Authorization", "Bearer " + access));
    }

    private ResultActions sessions(String access) throws Exception {
        return mockMvc.perform(get("/api/v1/sessions").header("Authorization", "Bearer " + access));
    }

    private ResultActions result(String access, String id) throws Exception {
        return mockMvc.perform(
                get("/api/v1/sessions/" + id + "/result")
                        .header("Authorization", "Bearer " + access));
    }

    private ResultActions submission(String access, String id) throws Exception {
        return mockMvc.perform(
                get("/api/v1/submissions/" + id).header("Authorization", "Bearer " + access));
    }

    private ResultActions progress(String access) throws Exception {
        return mockMvc.perform(
                get("/api/v1/me/progress").header("Authorization", "Bearer " + access));
    }

    private ResultActions question(String access, String slug) throws Exception {
        return mockMvc.perform(
                get("/api/v1/questions/" + slug).header("Authorization", "Bearer " + access));
    }

    private ResultActions track(String access) throws Exception {
        return mockMvc.perform(
                get("/api/v1/career/track").header("Authorization", "Bearer " + access));
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
                                                "{\"email\":\"s-"
                                                        + UUID.randomUUID()
                                                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
