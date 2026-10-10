package br.com.devready.api.gamification;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = "devready.seed.enabled=true")
class AchievementsTest extends IntegrationTest {

    private static final String QUESTION = "backend-junior-http-idempotency";

    @Test
    void theTenComeInTheOrderOfTheCatalogAndTheFirstAnswerUnlocksOne() throws Exception {
        String access = register();
        achievements(access)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].slug").value("primeira-questao"))
                .andExpect(jsonPath("$[0].name").value("Primeira questão"))
                .andExpect(jsonPath("$[0].description").value("Respondeu a primeira questão."))
                .andExpect(jsonPath("$[0].icon").value("check"))
                .andExpect(jsonPath("$[0].unlockedAt").value(nullValue()))
                .andExpect(jsonPath("$[5].slug").value("mil-xp"))
                .andExpect(jsonPath("$[5].name").value("Mil XP"))
                .andExpect(jsonPath("$[9].slug").value("primeira-simulacao"));

        String option =
                JsonPath.read(
                        body(
                                mockMvc.perform(
                                        get("/api/v1/questions/" + QUESTION)
                                                .header("Authorization", "Bearer " + access))),
                        "$.options[0].id");
        answer(access, option)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newAchievements[0]").value("primeira-questao"));
        answer(access, option)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newAchievements.length()").value(0));

        achievements(access).andExpect(jsonPath("$[0].unlockedAt").isString());
    }

    private ResultActions achievements(String access) throws Exception {
        return mockMvc.perform(
                get("/api/v1/me/achievements").header("Authorization", "Bearer " + access));
    }

    private ResultActions answer(String access, String option) throws Exception {
        return mockMvc.perform(
                post("/api/v1/questions/" + QUESTION + "/answer")
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content("{\"optionId\":\"" + option + "\"}"));
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }

    private String register() throws Exception {
        String response =
                mockMvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"c-"
                                                        + UUID.randomUUID()
                                                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
