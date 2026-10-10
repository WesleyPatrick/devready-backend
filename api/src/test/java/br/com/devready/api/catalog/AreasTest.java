package br.com.devready.api.catalog;

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

@TestPropertySource(properties = "devready.seed.enabled=true")
class AreasTest extends IntegrationTest {

    @Test
    void listsSeededAreasForAuthenticatedUsersOnly() throws Exception {
        mockMvc.perform(get("/api/v1/areas")).andExpect(status().isUnauthorized());

        String body =
                mockMvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"a-"
                                                        + UUID.randomUUID()
                                                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String access = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/v1/areas").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("backend"))
                .andExpect(jsonPath("$[0].questionCount").value(8))
                .andExpect(jsonPath("$[0].levels.length()").value(3))
                .andExpect(jsonPath("$[0].levels[0]").value("JUNIOR"))
                .andExpect(jsonPath("$[0].levels[1]").value("PLENO"))
                .andExpect(jsonPath("$[0].levels[2]").value("SENIOR"))
                .andExpect(jsonPath("$[1].levels.length()").value(3))
                .andExpect(jsonPath("$[2].levels.length()").value(3))
                .andExpect(jsonPath("$[1].slug").value("devops"))
                .andExpect(jsonPath("$[1].objectiveWeight").value(0.7))
                .andExpect(jsonPath("$[2].slug").value("frontend"));
    }
}
