package br.com.devready.api.identity;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.application.identity.port.OneTimeCodeStore;
import br.com.devready.application.identity.port.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

class InactiveAccountAndLogoutAllTest extends IntegrationTest {

    @Autowired private UserRepository users;
    @Autowired private OneTimeCodeStore oneTimeCodes;

    private String register(String email) throws Exception {
        return mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\""
                                                + email
                                                + "\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(APPLICATION_JSON).content(json));
    }

    @Test
    void inactiveAccountCannotLoginRefreshOrExchange() throws Exception {
        String email = "inativo-" + UUID.randomUUID() + "@example.com";
        String refreshToken = JsonPath.read(register(email), "$.refreshToken");
        var user = users.findByEmail(email).orElseThrow();
        user.deactivate();
        users.save(user);

        postJson("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"senha-forte\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Conta desativada"));
        postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}")
                .andExpect(status().isForbidden());
        String code = oneTimeCodes.issue(user.id());
        postJson("/api/v1/auth/exchange", "{\"code\":\"" + code + "\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshAfterLogoutAllIsUnauthorized() throws Exception {
        String body = register("todos-" + UUID.randomUUID() + "@example.com");
        String access = JsonPath.read(body, "$.accessToken");
        String refreshToken = JsonPath.read(body, "$.refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout-all").header("Authorization", "Bearer " + access))
                .andExpect(status().isNoContent());

        postJson("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refreshToken + "\"}")
                .andExpect(status().isUnauthorized());
    }
}
