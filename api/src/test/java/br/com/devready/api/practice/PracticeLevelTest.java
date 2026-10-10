package br.com.devready.api.practice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.ChallengeLanguage;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.ExecutionLimits;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.catalog.TestCase;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = "devready.seed.enabled=true")
class PracticeLevelTest extends IntegrationTest {

    private static final String SQL = "sql-e-modelagem-de-dados";

    @Autowired private AreaRepository areas;
    @Autowired private QuestionRepository questions;
    @Autowired private ChallengeRepository challenges;

    private record Items(Question pleno, Question senior, Challenge seniorChallenge) {}

    @Test
    void theListTheTabsAndTheLevelFilterFollowTheGoal() throws Exception {
        Items items = items();
        String junior = registerWithGoal("JUNIOR");
        String pleno = registerWithGoal("PLENO");
        String senior = registerWithGoal("SENIOR");
        long juniorTotal = total(list(junior, ""));

        assertThat(slugs(list(junior, "competency=" + SQL + "&size=50")))
                .doesNotContain(
                        items.pleno().slug(),
                        items.senior().slug(),
                        items.seniorChallenge().slug());
        assertThat(total(list(pleno, ""))).isEqualTo(juniorTotal + activeOfLevel(Level.PLENO));
        assertThat(total(list(senior, "")))
                .isEqualTo(juniorTotal + activeOfLevel(Level.PLENO) + activeOfLevel(Level.SENIOR));
        assertThat(total(list(pleno, "status=NOT_STARTED"))).isEqualTo(total(list(pleno, "")));

        String plenoOnly =
                body(
                        list(pleno, "level=PLENO&size=50")
                                .andExpect(status().isOk())
                                .andExpect(
                                        jsonPath("$.totalItems")
                                                .value((int) activeOfLevel(Level.PLENO))));
        assertThat(JsonPath.<List<String>>read(plenoOnly, "$.items[*].level"))
                .containsOnly("PLENO");
        assertThat(JsonPath.<List<String>>read(plenoOnly, "$.items[*].slug"))
                .contains(items.pleno().slug());
        list(pleno, "level=SENIOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        assertThat(slugs(list(senior, "level=SENIOR&size=50")))
                .contains(items.senior().slug(), items.seniorChallenge().slug());
        list(junior, "level=MESTRE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(
                        jsonPath("$.detail").value("O parâmetro level está em formato inválido."));
    }

    @Test
    void theTabsCountOnlyTheVisibleAndLoweringTheGoalKeepsTheXp() throws Exception {
        Items items = items();
        String pleno = registerWithGoal("PLENO");

        answer(pleno, items.pleno().slug(), items.pleno().correctOption().id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.xpGained").value(30));
        list(pleno, "status=SOLVED").andExpect(jsonPath("$.totalItems").value(1));

        goal(pleno, "JUNIOR");

        list(pleno, "status=SOLVED").andExpect(jsonPath("$.totalItems").value(0));
        assertThat(slugs(list(pleno, "competency=" + SQL + "&size=50")))
                .doesNotContain(items.pleno().slug());
        question(pleno, items.pleno().slug()).andExpect(status().isNotFound());
        progress(pleno).andExpect(status().isOk()).andExpect(jsonPath("$.xp").value(30));
    }

    @Test
    void aQuestionOrAChallengeAboveTheGoalIsNotFound() throws Exception {
        Items items = items();
        String seniorQuestion = items.senior().slug();
        String seniorChallenge = items.seniorChallenge().slug();
        String pleno = registerWithGoal("PLENO");

        question(pleno, items.pleno().slug())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("PLENO"));
        question(pleno, seniorQuestion)
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Questão não encontrada: " + seniorQuestion + "."));
        answer(pleno, seniorQuestion, items.senior().correctOption().id())
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Questão não encontrada: " + seniorQuestion + "."));
        challenge(pleno, seniorChallenge)
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Desafio não encontrado: " + seniorChallenge + "."));
        code(pleno, seniorChallenge, "runs").andExpect(status().isNotFound());
        code(pleno, seniorChallenge, "submissions").andExpect(status().isNotFound());

        String senior = registerWithGoal("SENIOR");
        question(senior, seniorQuestion)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("SENIOR"));
        challenge(senior, seniorChallenge).andExpect(status().isOk());

        question(register(), items.pleno().slug()).andExpect(status().isNotFound());
    }

    @Test
    void theCompetencyPanelShowsTheVisibleItemsWithTheirLevel() throws Exception {
        Items items = items();
        String junior = registerWithGoal("JUNIOR");
        String senior = registerWithGoal("SENIOR");

        String juniorPanel = body(competency(junior, SQL).andExpect(status().isOk()));
        assertThat(JsonPath.<List<String>>read(juniorPanel, "$.questions[*].slug"))
                .doesNotContain(items.pleno().slug(), items.senior().slug());
        assertThat(JsonPath.<List<String>>read(juniorPanel, "$.questions[*].level"))
                .containsOnly("JUNIOR");
        assertThat(JsonPath.<List<String>>read(juniorPanel, "$.challenges[*].slug"))
                .doesNotContain(items.seniorChallenge().slug());

        String seniorPanel = body(competency(senior, SQL).andExpect(status().isOk()));
        assertThat(levelOf(seniorPanel, "questions", items.pleno().slug()))
                .containsExactly("PLENO");
        assertThat(levelOf(seniorPanel, "questions", items.senior().slug()))
                .containsExactly("SENIOR");
        assertThat(levelOf(seniorPanel, "challenges", items.seniorChallenge().slug()))
                .containsExactly("SENIOR");
    }

    private static List<String> levelOf(String panel, String list, String slug) {
        return JsonPath.read(panel, "$." + list + "[?(@.slug == '" + slug + "')].level");
    }

    private UUID backend() {
        return areas.findBySlug("backend").orElseThrow().id();
    }

    private Items items() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        return new Items(
                questions.save(newQuestion("zz-nivel-pleno-" + s, Level.PLENO)),
                questions.save(newQuestion("zz-nivel-senior-" + s, Level.SENIOR)),
                challenges.save(newChallenge("zz-nivel-desafio-" + s, Level.SENIOR)));
    }

    private long activeOfLevel(Level level) {
        return questions.countActiveByAreaAndLevel(backend(), level)
                + challenges.countActiveByAreaAndLevel(backend(), level);
    }

    private Question newQuestion(String slug, Level level) {
        return new Question(
                UUID.randomUUID(),
                slug,
                "Questão " + slug,
                backend(),
                level,
                Difficulty.MEDIUM,
                "Enunciado da questão de nível, com o tamanho mínimo.",
                "Explicação da questão de nível, com o tamanho mínimo.",
                List.of(
                        new QuestionOption(UUID.randomUUID(), "Certa", true, 0),
                        new QuestionOption(UUID.randomUUID(), "Errada", false, 1)),
                Set.of("sql"),
                true);
    }

    private Challenge newChallenge(String slug, Level level) {
        return new Challenge(
                UUID.randomUUID(),
                slug,
                "Desafio " + slug,
                backend(),
                level,
                Difficulty.HARD,
                "Enunciado do desafio de nível.",
                "Abordagem do desafio de nível.",
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
                Set.of("sql"),
                true);
    }

    private ResultActions list(String access, String query) throws Exception {
        return mockMvc.perform(
                get("/api/v1/questions" + (query.isEmpty() ? "" : "?" + query))
                        .header("Authorization", "Bearer " + access));
    }

    private ResultActions question(String access, String slug) throws Exception {
        return mockMvc.perform(
                get("/api/v1/questions/" + slug).header("Authorization", "Bearer " + access));
    }

    private ResultActions answer(String access, String slug, UUID optionId) throws Exception {
        return mockMvc.perform(
                post("/api/v1/questions/" + slug + "/answer")
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content("{\"optionId\":\"" + optionId + "\"}"));
    }

    private ResultActions challenge(String access, String slug) throws Exception {
        return mockMvc.perform(
                get("/api/v1/challenges/" + slug).header("Authorization", "Bearer " + access));
    }

    private ResultActions code(String access, String slug, String kind) throws Exception {
        return mockMvc.perform(
                post("/api/v1/challenges/" + slug + "/" + kind)
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content("{\"language\":\"JAVA\",\"sourceCode\":\"class Solution {}\"}"));
    }

    private ResultActions competency(String access, String slug) throws Exception {
        return mockMvc.perform(
                get("/api/v1/career/track/competencies/" + slug)
                        .header("Authorization", "Bearer " + access));
    }

    private ResultActions progress(String access) throws Exception {
        return mockMvc.perform(
                get("/api/v1/me/progress").header("Authorization", "Bearer " + access));
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    private static long total(ResultActions actions) throws Exception {
        return JsonPath.<Integer>read(body(actions.andExpect(status().isOk())), "$.totalItems");
    }

    private static List<String> slugs(ResultActions actions) throws Exception {
        return JsonPath.read(body(actions.andExpect(status().isOk())), "$.items[*].slug");
    }

    private String registerWithGoal(String seniority) throws Exception {
        String access = register();
        goal(access, seniority);
        return access;
    }

    private void goal(String access, String seniority) throws Exception {
        mockMvc.perform(
                        put("/api/v1/career/profile")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"companies\":[\"nubank\"],\"stack\":\"java-spring\","
                                                + "\"seniority\":\""
                                                + seniority
                                                + "\"}"))
                .andExpect(status().isOk());
    }

    private String register() throws Exception {
        String response =
                mockMvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"n-"
                                                        + UUID.randomUUID()
                                                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
