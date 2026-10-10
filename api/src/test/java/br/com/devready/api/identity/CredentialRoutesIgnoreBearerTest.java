package br.com.devready.api.identity;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.api.TestJwts;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CredentialRoutesIgnoreBearerTest extends IntegrationTest {

    private String register() throws Exception {
        return mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"b-"
                                                + UUID.randomUUID()
                                                + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void refreshAndLogoutWorkWithAnExpiredBearerInTheHeader() throws Exception {
        String expired = "Bearer " + TestJwts.expired();
        String refreshToken = JsonPath.read(register(), "$.refreshToken");

        String renewed =
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .header("Authorization", expired)
                                        .contentType(APPLICATION_JSON)
                                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .header("Authorization", expired)
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"refreshToken\":\""
                                                + JsonPath.read(renewed, "$.refreshToken")
                                                + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void registerWorksWithAnExpiredBearerInTheHeader() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .header("Authorization", "Bearer " + TestJwts.expired())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"r-"
                                                + UUID.randomUUID()
                                                + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void routesThatNeedTheUserStillValidateTheBearer() throws Exception {
        String expired = "Bearer " + TestJwts.expired();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", expired))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/logout-all").header("Authorization", expired))
                .andExpect(status().isUnauthorized());
    }
}
