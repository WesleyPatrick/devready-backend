package br.com.devready.api.config;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "devready.admin.email=admin@devready.local",
            "devready.admin.password=senha-admin-1"
        })
class AdminBootstrapTest extends IntegrationTest {

    @Test
    void adminFromConfigurationCanLoginAndReachAdminRoutes() throws Exception {
        String body =
                mockMvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"admin@devready.local\",\"password\":\"senha-admin-1\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.user.role").value("ADMIN"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String access = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1));
        mockMvc.perform(get("/api/v1/admin/anything").header("Authorization", "Bearer " + access))
                .andExpect(status().isNotFound());
    }
}
