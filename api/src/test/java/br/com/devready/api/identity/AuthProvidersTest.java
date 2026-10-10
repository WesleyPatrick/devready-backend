package br.com.devready.api.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import org.junit.jupiter.api.Test;

class AuthProvidersTest extends IntegrationTest {

    @Test
    void listsTheConfiguredProvidersWithoutLoginAndIgnoringAnOldToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers.length()").value(2))
                .andExpect(jsonPath("$.providers[0]").value("GOOGLE"))
                .andExpect(jsonPath("$.providers[1]").value("GITHUB"));
        mockMvc.perform(
                        get("/api/v1/auth/providers")
                                .header("Authorization", "Bearer token-vencido"))
                .andExpect(status().isOk());
    }

    @Test
    void anUnknownProviderGoesBackToTheLoginWithAnError() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/facebook"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:5173/login?error=social"));
    }
}
