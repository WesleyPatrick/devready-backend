package br.com.devready.api.practice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = "devready.seed.enabled=true")
class PracticeTest extends IntegrationTest {

    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private static final String JUNIOR_JAVA =
            "{\"companies\":[\"amazon\",\"google\",\"nubank\",\"meta\"],"
                    + "\"stack\":\"java-spring\",\"seniority\":\"JUNIOR\"}";

    private static final String DAILY = "backend-junior-java-equals-hashcode";

    private static final List<String> LIST_ORDER =
            List.of(
                    "valid-parentheses",
                    "first-unique-char",
                    "two-sum",
                    "merge-sorted-arrays",
                    DAILY,
                    "backend-junior-spring-constructor-injection",
                    "backend-junior-sql-foreign-key",
                    "backend-junior-sql-inner-join",
                    "backend-junior-sql-index",
                    "backend-junior-orm-n-plus-one",
                    "backend-junior-java-checked-exceptions",
                    "backend-junior-unit-test-doubles",
                    "backend-junior-http-idempotency",
                    "backend-junior-http-401-vs-403",
                    "backend-junior-git-revert",
                    "backend-junior-password-hashing");

    @Test
    void everyRouteNeedsAToken() throws Exception {
        mockMvc.perform(get("/api/v1/questions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/questions/daily")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/questions/" + DAILY)).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/v1/questions/" + DAILY + "/answer")
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/progress")).andExpect(status().isUnauthorized());
    }

    @Test
    void listAndDailyNeedAProfileButAQuestionAndTheProgressDoNot() throws Exception {
        String access = register();

        list(access, "")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Perfil de carreira ainda não preenchido."));
        daily(access)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Perfil de carreira ainda não preenchido."));
        question(access, "backend-junior-password-hashing")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.competency.slug").value("seguranca-de-aplicacoes"))
                .andExpect(jsonPath("$.competency.category").value("pratica"))
                .andExpect(jsonPath("$.status").value("NOT_STARTED"));
        progress(access)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xp").value(0))
                .andExpect(jsonPath("$.streakDays").value(0))
                .andExpect(jsonPath("$.dailyGoal.done").value(0))
                .andExpect(jsonPath("$.dailyGoal.target").value(3))
                .andExpect(jsonPath("$.activity.length()").value(35));
    }

    @Test
    void listsTheProblemsOfTheStackAreaInTheOrderOfTheTrack() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        String first =
                body(
                        list(access, "")
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.page").value(1))
                                .andExpect(jsonPath("$.totalPages").value(2))
                                .andExpect(jsonPath("$.totalItems").value(16))
                                .andExpect(jsonPath("$.items.length()").value(10))
                                .andExpect(jsonPath("$.items[0].type").value("CHALLENGE"))
                                .andExpect(
                                        jsonPath("$.items[0].title")
                                                .value("Parênteses balanceados"))
                                .andExpect(jsonPath("$.items[0].difficulty").value("EASY"))
                                .andExpect(jsonPath("$.items[0].status").value("NOT_STARTED"))
                                .andExpect(jsonPath("$.items[0].acceptance").value(nullValue()))
                                .andExpect(
                                        jsonPath("$.items[0].competency.slug")
                                                .value("estruturas-de-dados-e-algoritmos"))
                                .andExpect(
                                        jsonPath("$.items[0].competency.name")
                                                .value("Estruturas de dados e algoritmos"))
                                .andExpect(
                                        jsonPath("$.items[0].competency.category")
                                                .value("fundamento"))
                                .andExpect(jsonPath("$.items[0].askedBy[0]").value("amazon"))
                                .andExpect(jsonPath("$.items[0].askedBy[1]").value("google"))
                                .andExpect(jsonPath("$.items[0].askedBy[2]").value("nubank"))
                                .andExpect(jsonPath("$.items[4].type").value("QUESTION"))
                                .andExpect(jsonPath("$.items[4].difficulty").value("HARD"))
                                .andExpect(
                                        jsonPath("$.items[5].competency.slug")
                                                .value("design-de-sistemas")));
        String second =
                body(
                        list(access, "page=2")
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.page").value(2))
                                .andExpect(jsonPath("$.items.length()").value(6))
                                .andExpect(jsonPath("$.items[4].askedBy.length()").value(1))
                                .andExpect(jsonPath("$.items[4].askedBy[0]").value("amazon"))
                                .andExpect(
                                        jsonPath("$.items[5].competency.slug")
                                                .value("seguranca-de-aplicacoes"))
                                .andExpect(jsonPath("$.items[5].askedBy.length()").value(0)));
        List<String> firstPage = JsonPath.read(first, "$.items[*].slug");
        List<String> secondPage = JsonPath.read(second, "$.items[*].slug");
        List<String> slugs = new ArrayList<>(firstPage);
        slugs.addAll(secondPage);
        assertThat(slugs).containsExactlyElementsOf(LIST_ORDER);
    }

    @Test
    void filtersAndPages() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        list(access, "type=CHALLENGE").andExpect(jsonPath("$.totalItems").value(4));
        list(access, "type=QUESTION&difficulty=HARD")
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].slug").value(DAILY))
                .andExpect(jsonPath("$.items[1].slug").value("backend-junior-orm-n-plus-one"));
        list(access, "competency=sql-e-modelagem-de-dados")
                .andExpect(jsonPath("$.totalItems").value(4));
        list(access, "company=amazon").andExpect(jsonPath("$.totalItems").value(15));
        list(access, "company=nubank").andExpect(jsonPath("$.totalItems").value(14));
        list(access, "company=meta")
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(1));
        list(access, "status=SOLVED").andExpect(jsonPath("$.totalItems").value(0));
        list(access, "size=5&page=4")
                .andExpect(jsonPath("$.totalPages").value(4))
                .andExpect(jsonPath("$.items.length()").value(1));
        list(access, "page=9").andExpect(jsonPath("$.page").value(2));
        for (String name : List.of("type", "status", "difficulty")) {
            list(access, name + "=OUTRO")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Requisição inválida"))
                    .andExpect(
                            jsonPath("$.detail")
                                    .value("O parâmetro " + name + " está em formato inválido."));
        }
    }

    @Test
    void answeringChangesTheStatusTheProgressAndTheTrackAndKeepsTheDailyQuestion()
            throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);
        String daily =
                body(
                        daily(access)
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.slug").value(DAILY))
                                .andExpect(
                                        jsonPath("$.title").value("equals sem hashCode em HashMap"))
                                .andExpect(
                                        jsonPath("$.competency.slug")
                                                .value("estruturas-de-dados-e-algoritmos"))
                                .andExpect(jsonPath("$.difficulty").value("HARD"))
                                .andExpect(jsonPath("$.status").value("NOT_STARTED"))
                                .andExpect(jsonPath("$.estimatedMinutes").value(6))
                                .andExpect(jsonPath("$.xp").value(40))
                                .andExpect(jsonPath("$.options.length()").value(4))
                                .andExpect(
                                        jsonPath(
                                                "$.statement",
                                                startsWith("Você sobrescreveu `equals`"))));
        List<String> options = JsonPath.read(daily, "$.options[*].id");

        String wrong =
                body(
                        answer(access, DAILY, options.get(0))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.correct").value(false))
                                .andExpect(jsonPath("$.xpGained").value(0))
                                .andExpect(
                                        jsonPath(
                                                "$.explanation",
                                                startsWith("O `HashMap` usa primeiro"))));
        String correctId = JsonPath.read(wrong, "$.correctOptionId");
        assertThat(correctId).isEqualTo(options.get(2));
        question(access, DAILY).andExpect(jsonPath("$.status").value("ATTEMPTED"));
        track(access).andExpect(jsonPath("$.competencies[0].status").value("IN_PROGRESS"));

        answer(access, DAILY, correctId)
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.xpGained").value(40));
        answer(access, DAILY, correctId).andExpect(jsonPath("$.xpGained").value(0));
        answer(access, DAILY, options.get(1)).andExpect(jsonPath("$.correct").value(false));

        question(access, DAILY).andExpect(jsonPath("$.status").value("SOLVED"));
        list(access, "status=SOLVED")
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].slug").value(DAILY));
        list(access, "status=ATTEMPTED").andExpect(jsonPath("$.totalItems").value(0));
        daily(access)
                .andExpect(jsonPath("$.slug").value(DAILY))
                .andExpect(jsonPath("$.status").value("SOLVED"));

        String before = LocalDate.now(BRASILIA).toString();
        String progress =
                body(
                        progress(access)
                                .andExpect(jsonPath("$.xp").value(40))
                                .andExpect(jsonPath("$.streakDays").value(1))
                                .andExpect(jsonPath("$.dailyGoal.done").value(3))
                                .andExpect(jsonPath("$.dailyGoal.target").value(3))
                                .andExpect(jsonPath("$.activity.length()").value(35))
                                .andExpect(jsonPath("$.activity[34].count").value(4)));
        String after = LocalDate.now(BRASILIA).toString();
        String today = JsonPath.read(progress, "$.activity[34].date");
        assertThat(today).isIn(before, after);

        track(access)
                .andExpect(jsonPath("$.plan.doneCompetencies").value(0))
                .andExpect(jsonPath("$.plan.byDifficulty.MEDIUM.done").value(0))
                .andExpect(jsonPath("$.plan.byDifficulty.HARD.done").value(0))
                .andExpect(jsonPath("$.competencies[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.competencies[1].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.competencies[3].slug").value("java"))
                .andExpect(jsonPath("$.competencies[3].status").value("IN_PROGRESS"));
        competency(access, "java")
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(
                        jsonPath("$.questions[0].slug")
                                .value("backend-junior-java-checked-exceptions"))
                .andExpect(jsonPath("$.questions[0].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.questions[1].slug").value(DAILY))
                .andExpect(jsonPath("$.questions[1].status").value("SOLVED"));
    }

    @Test
    void theRateNeedsFivePeopleAndCountsTheirFirstAnswer() throws Exception {
        String slug = "backend-junior-orm-n-plus-one";
        String viewer = registerWithProfile(JUNIOR_JAVA);
        List<String> options = JsonPath.read(body(question(viewer, slug)), "$.options[*].id");
        String first = register();
        String correct =
                JsonPath.read(body(answer(first, slug, options.get(0))), "$.correctOptionId");
        assertThat(correct).isEqualTo(options.get(2));
        answer(first, slug, correct).andExpect(jsonPath("$.correct").value(true));
        for (int i = 0; i < 3; i++) {
            answer(register(), slug, correct).andExpect(jsonPath("$.correct").value(true));
        }

        assertThat(acceptanceOf(viewer, slug)).isNull();

        answer(register(), slug, options.get(0)).andExpect(jsonPath("$.correct").value(false));

        assertThat(acceptanceOf(viewer, slug)).isEqualTo(0.6);
    }

    @Test
    void answerErrors() throws Exception {
        String access = register();
        List<String> otherOptions = JsonPath.read(body(question(access, DAILY)), "$.options[*].id");

        answer(access, "backend-junior-sql-index", otherOptions.get(0))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(
                        jsonPath("$.detail").value("A alternativa escolhida não é desta questão."));
        answer(access, "backend-junior-sql-index", UUID.randomUUID().toString())
                .andExpect(status().isUnprocessableContent());
        mockMvc.perform(
                        post("/api/v1/questions/backend-junior-sql-index/answer")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("optionId"))
                .andExpect(jsonPath("$.errors[0].message").value("Escolha uma alternativa."));
        answer(access, "two-sum", UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"));
        answer(access, "nao-existe", UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Questão não encontrada: nao-existe."));
        question(access, "nao-existe").andExpect(status().isNotFound());
        question(access, "two-sum")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Questão não encontrada: two-sum."));
    }

    @Test
    void anEmptyTrackListsTheAreaButHasNoDailyQuestion() throws Exception {
        String access =
                registerWithProfile(
                        "{\"companies\":[\"meta\",\"nubank\"],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}");

        list(access, "size=50")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(16))
                .andExpect(jsonPath("$.items[0].askedBy.length()").value(0))
                .andExpect(jsonPath("$.items[?(@.slug =~ /^backend-.*/)]").isEmpty())
                .andExpect(
                        jsonPath("$.items[?(@.slug == 'frontend-junior-css-box-model')]")
                                .isNotEmpty());
        daily(access)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(
                        jsonPath("$.detail").value("Sua trilha ainda não tem questão objetiva."));
    }

    private Object acceptanceOf(String access, String slug) throws Exception {
        String page = body(list(access, "competency=sql-e-modelagem-de-dados"));
        List<Object> values =
                JsonPath.read(page, "$.items[?(@.slug == '" + slug + "')].acceptance");
        assertThat(values).hasSize(1);
        return values.get(0);
    }

    private ResultActions list(String access, String query) throws Exception {
        return mockMvc.perform(
                get("/api/v1/questions" + (query.isEmpty() ? "" : "?" + query))
                        .header("Authorization", "Bearer " + access));
    }

    private ResultActions daily(String access) throws Exception {
        return mockMvc.perform(
                get("/api/v1/questions/daily").header("Authorization", "Bearer " + access));
    }

    private ResultActions question(String access, String slug) throws Exception {
        return mockMvc.perform(
                get("/api/v1/questions/" + slug).header("Authorization", "Bearer " + access));
    }

    private ResultActions answer(String access, String slug, String optionId) throws Exception {
        return mockMvc.perform(
                post("/api/v1/questions/" + slug + "/answer")
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content("{\"optionId\":\"" + optionId + "\"}"));
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
                                                "{\"email\":\"p-"
                                                        + UUID.randomUUID()
                                                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
