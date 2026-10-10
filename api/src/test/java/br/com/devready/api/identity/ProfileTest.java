package br.com.devready.api.identity;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.application.identity.dto.SocialLoginCommand;
import br.com.devready.application.identity.usecase.LoginWithSocialAccountUseCase;
import br.com.devready.domain.identity.SocialProvider;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProfileTest extends IntegrationTest {

    @Autowired private LoginWithSocialAccountUseCase loginWithSocialAccount;

    private static String uniqueEmail() {
        return "p-" + UUID.randomUUID() + "@example.com";
    }

    private String register(String email) throws Exception {
        return mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\""
                                                + email
                                                + "\",\"password\":\"senha-atual\",\"displayName\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private org.springframework.test.web.servlet.ResultActions patchMe(String access, String json)
            throws Exception {
        return mockMvc.perform(
                patch("/api/v1/me")
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content(json));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken)
            throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(
                                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void updatesNameWithoutPassword() throws Exception {
        String body = register(uniqueEmail());
        String access = JsonPath.read(body, "$.accessToken");
        String refreshToken = JsonPath.read(body, "$.refreshToken");

        patchMe(access, "{\"displayName\":\"Ana Souza\",\"avatarUrl\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ana Souza"))
                .andExpect(jsonPath("$.hasPassword").value(true));

        refresh(refreshToken).andExpect(status().isOk());
    }

    @Test
    void changesPasswordWithTheRightCurrentPasswordAndEndsTheSessions() throws Exception {
        String email = uniqueEmail();
        String body = register(email);
        String access = JsonPath.read(body, "$.accessToken");
        String refreshToken = JsonPath.read(body, "$.refreshToken");

        patchMe(
                        access,
                        "{\"displayName\":\"Ana\",\"newPassword\":\"nova-senha-1\","
                                + "\"currentPassword\":\"senha-atual\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPassword").value(true));

        refresh(refreshToken).andExpect(status().isUnauthorized());
        login(email, "senha-atual").andExpect(status().isUnauthorized());
        login(email, "nova-senha-1").andExpect(status().isOk());
    }

    @Test
    void theAvatarOnlyAcceptsHttps() throws Exception {
        String access = JsonPath.read(register(uniqueEmail()), "$.accessToken");

        for (String url :
                new String[] {
                    "http://exemplo.com/foto.png", "javascript:alert(1)", "//exemplo.com/a.png"
                }) {
            patchMe(access, "{\"displayName\":\"Ana\",\"avatarUrl\":\"" + url + "\"}")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.errors[0].field").value("avatarUrl"))
                    .andExpect(
                            jsonPath("$.errors[0].message")
                                    .value("Use um endereço que comece com https://."));
        }
        patchMe(access, "{\"displayName\":\"Ana\",\"avatarUrl\":\"https://exemplo.com/a.png\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://exemplo.com/a.png"));
        patchMe(access, "{\"displayName\":\"Ana\",\"avatarUrl\":\"\"}").andExpect(status().isOk());
    }

    @Test
    void wrongCurrentPasswordIsAFieldError() throws Exception {
        String email = uniqueEmail();
        String body = register(email);
        String access = JsonPath.read(body, "$.accessToken");
        String refreshToken = JsonPath.read(body, "$.refreshToken");

        patchMe(
                        access,
                        "{\"displayName\":\"Ana\",\"newPassword\":\"nova-senha-1\","
                                + "\"currentPassword\":\"errada-123\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[0].field").value("currentPassword"))
                .andExpect(jsonPath("$.errors[0].message").value("Senha atual incorreta."));

        refresh(refreshToken).andExpect(status().isOk());
        login(email, "senha-atual").andExpect(status().isOk());
    }

    @Test
    void missingCurrentPasswordIsAFieldError() throws Exception {
        String access = JsonPath.read(register(uniqueEmail()), "$.accessToken");

        patchMe(access, "{\"displayName\":\"Ana\",\"newPassword\":\"nova-senha-1\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("currentPassword"))
                .andExpect(jsonPath("$.errors[0].message").value("Informe a senha atual."));
    }

    @Test
    void weakNewPasswordIsRejected() throws Exception {
        String access = JsonPath.read(register(uniqueEmail()), "$.accessToken");

        patchMe(
                        access,
                        "{\"displayName\":\"Ana\",\"newPassword\":\"curta\","
                                + "\"currentPassword\":\"senha-atual\"}")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void socialAccountDefinesAPasswordWithoutCurrentPassword() throws Exception {
        String email = uniqueEmail();
        String code =
                loginWithSocialAccount
                        .execute(
                                new SocialLoginCommand(
                                        SocialProvider.GOOGLE, "g-" + email, email, "Bia", null))
                        .value();
        String body =
                mockMvc.perform(
                                post("/api/v1/auth/exchange")
                                        .contentType(APPLICATION_JSON)
                                        .content("{\"code\":\"" + code + "\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.user.hasPassword").value(false))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String access = JsonPath.read(body, "$.accessToken");
        String refreshToken = JsonPath.read(body, "$.refreshToken");

        patchMe(access, "{\"displayName\":\"Bia\",\"newPassword\":\"primeira-senha\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPassword").value(true));

        refresh(refreshToken).andExpect(status().isUnauthorized());
        login(email, "primeira-senha").andExpect(status().isOk());
    }
}
