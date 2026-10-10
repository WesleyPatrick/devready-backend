package br.com.devready.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthFlowTest extends IntegrationTest {

    private static String uniqueEmail() {
        return "u-" + UUID.randomUUID() + "@example.com";
    }

    private String register(String email, String password) throws Exception {
        return mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\""
                                                + email
                                                + "\",\"password\":\""
                                                + password
                                                + "\",\"displayName\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.user.hasPassword").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void registerLoginMeRefreshAndLogout() throws Exception {
        String email = uniqueEmail();
        String body = register(email, "senha-forte");
        String access = JsonPath.read(body, "$.accessToken");
        String refresh = JsonPath.read(body, "$.refreshToken");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.providers").isEmpty());

        String loginBody =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\""
                                                        + email.toUpperCase()
                                                        + "\",\"password\":\"senha-forte\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat((String) JsonPath.read(loginBody, "$.accessToken")).isNotBlank();

        String refreshed =
                mockMvc.perform(
                                post("/api/v1/auth/refresh")
                                        .contentType(APPLICATION_JSON)
                                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String newRefresh = JsonPath.read(refreshed, "$.refreshToken");
        assertThat(newRefresh).isNotEqualTo(refresh);

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
                .andExpect(status().isUnauthorized());

        String again =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\""
                                                        + email
                                                        + "\",\"password\":\"senha-forte\"}"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String r2 = JsonPath.read(again, "$.refreshToken");
        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .contentType(APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + r2 + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + r2 + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationAndConflictErrorsAreProblemDetails() throws Exception {
        String email = uniqueEmail();
        register(email, "senha-forte");

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\""
                                                + email
                                                + "\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("E-mail já cadastrado"))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Já existe uma conta com o e-mail " + email + "."));

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"nao-e-email\",\"password\":\"curta\",\"displayName\":\"\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[*].field").isArray())
                .andExpect(
                        jsonPath("$.errors[?(@.field == 'email')].message")
                                .value("E-mail inválido."));

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("{\"email\":\"\",\"password\":\"errada\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(
                        jsonPath("$.errors[?(@.field == 'email')].message")
                                .value("Informe o e-mail."));

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("{\"email\":\"" + email + "\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void protectedRoutesRequireTokenAndAdminRoutesRequireRole() throws Exception {
        String access = JsonPath.read(register(uniqueEmail(), "senha-forte"), "$.accessToken");

        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/logout-all")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer nao.e.jwt"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/anything").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/logout-all").header("Authorization", "Bearer " + access))
                .andExpect(status().isNoContent());
    }

    @Test
    void passwordOverSeventyTwoBytesIsAFieldProblemNotAServerError() throws Exception {
        String longPassword = "ç".repeat(72);
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\""
                                                + uniqueEmail()
                                                + "\",\"password\":\""
                                                + longPassword
                                                + "\",\"displayName\":\"Ana\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("72 bytes")));

        String email = uniqueEmail();
        register(email, "senha-forte");
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\""
                                                + email
                                                + "\",\"password\":\""
                                                + longPassword
                                                + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
