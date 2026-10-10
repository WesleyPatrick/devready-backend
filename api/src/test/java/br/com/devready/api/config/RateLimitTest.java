package br.com.devready.api.config;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "devready.auth.rate-limit.enabled=true",
            "devready.auth.rate-limit.requests-per-minute=3"
        })
class RateLimitTest extends IntegrationTest {

    @Test
    void fourthLoginAttemptInAMinuteIsRejected() throws Exception {
        String body = "{\"email\":\"x@example.com\",\"password\":\"whatever-1\"}";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            post("/api/v1/auth/login")
                                    .with(
                                            r -> {
                                                r.setRemoteAddr("10.0.0.7");
                                                return r;
                                            })
                                    .contentType(APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .with(
                                        r -> {
                                            r.setRemoteAddr("10.0.0.7");
                                            return r;
                                        })
                                .contentType(APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Muitas tentativas"));
        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .with(
                                        r -> {
                                            r.setRemoteAddr("10.0.0.8");
                                            return r;
                                        })
                                .contentType(APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void runsAndSubmissionsHaveTheirOwnLimitPerIpWhateverTheChallenge() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            post("/api/v1/challenges/two-sum/runs")
                                    .with(
                                            r -> {
                                                r.setRemoteAddr("10.0.0.21");
                                                return r;
                                            })
                                    .contentType(APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(
                        post("/api/v1/challenges/valid-parentheses/runs")
                                .with(
                                        r -> {
                                            r.setRemoteAddr("10.0.0.21");
                                            return r;
                                        })
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Muitas tentativas"));
        mockMvc.perform(
                        post("/api/v1/challenges/two-sum/submissions")
                                .with(
                                        r -> {
                                            r.setRemoteAddr("10.0.0.21");
                                            return r;
                                        })
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void runsAndSubmissionsOfASessionAlsoHaveTheirLimit() throws Exception {
        String path = "/api/v1/sessions/" + java.util.UUID.randomUUID() + "/challenges/1/runs";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            post(path)
                                    .with(
                                            r -> {
                                                r.setRemoteAddr("10.0.0.31");
                                                return r;
                                            })
                                    .contentType(APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(
                        post(path)
                                .with(
                                        r -> {
                                            r.setRemoteAddr("10.0.0.31");
                                            return r;
                                        })
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(
                        post(path.replace("/runs", "/submissions"))
                                .with(
                                        r -> {
                                            r.setRemoteAddr("10.0.0.31");
                                            return r;
                                        })
                                .contentType(APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetsHaveTheirOwnLimit() throws Exception {
        String path = "/api/v1/admin/users/" + java.util.UUID.randomUUID() + "/password-reset";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            post(path)
                                    .with(
                                            r -> {
                                                r.setRemoteAddr("10.0.0.41");
                                                return r;
                                            }))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(
                        post("/api/v1/admin/users/"
                                        + java.util.UUID.randomUUID()
                                        + "/password-reset")
                                .with(
                                        r -> {
                                            r.setRemoteAddr("10.0.0.41");
                                            return r;
                                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Muitas tentativas"));
    }
}
