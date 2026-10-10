package br.com.devready.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.api.HttpIntegrationTest;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = {
            "devready.auth.rate-limit.enabled=true",
            "devready.auth.rate-limit.requests-per-minute=3"
        })
class RateLimitHttpTest extends HttpIntegrationTest {

    private static final String LOGIN = "{\"email\":\"x@example.com\",\"password\":\"whatever-1\"}";

    private int post(String path, String forwardedFor, String body) {
        return http.post()
                .uri(URI.create(baseUrl() + path))
                .header("X-Forwarded-For", forwardedFor)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode()
                .value();
    }

    @Test
    void percentEncodedPathUsesTheSameBucketAsLogin() {
        for (int i = 0; i < 3; i++) {
            assertThat(post("/api/v1/auth/login", "203.0.113.31", LOGIN)).isEqualTo(401);
        }

        assertThat(post("/api/v1/auth/%6cogin", "203.0.113.31", LOGIN)).isEqualTo(429);
    }

    @Test
    void loginAndRegisterHaveSeparateBuckets() {
        for (int i = 0; i < 3; i++) {
            assertThat(post("/api/v1/auth/login", "203.0.113.32", LOGIN)).isEqualTo(401);
        }
        assertThat(post("/api/v1/auth/login", "203.0.113.32", LOGIN)).isEqualTo(429);

        assertThat(post("/api/v1/auth/register", "203.0.113.32", "{}")).isEqualTo(422);
    }

    @Test
    void spoofedFirstForwardedHopDoesNotResetTheLimit() {
        for (int i = 0; i < 3; i++) {
            assertThat(post("/api/v1/auth/login", "198.51.100." + i + ", 203.0.113.41", LOGIN))
                    .isEqualTo(401);
        }

        assertThat(post("/api/v1/auth/login", "198.51.100.99, 203.0.113.41", LOGIN)).isEqualTo(429);
    }
}
