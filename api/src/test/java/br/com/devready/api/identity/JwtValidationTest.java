package br.com.devready.api.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.api.TestJwts;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class JwtValidationTest extends IntegrationTest {

    private static final String OTHER_SECRET = "outra-chave-outra-chave-outra-chave-outra-chave";

    private static String token(String secret, String issuer, Instant expiresAt) {
        return TestJwts.sign(secret, issuer, expiresAt);
    }

    private static Instant inTenMinutes() {
        return Instant.now().plus(Duration.ofMinutes(10));
    }

    private org.springframework.test.web.servlet.ResultActions areasWith(String token)
            throws Exception {
        return mockMvc.perform(get("/api/v1/areas").header("Authorization", "Bearer " + token));
    }

    @Test
    void acceptsATokenSignedWithOurKeyAndIssuer() throws Exception {
        areasWith(token(TestJwts.TEST_SECRET, "devready", inTenMinutes()))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        areasWith(
                        token(
                                TestJwts.TEST_SECRET,
                                "devready",
                                Instant.now().minus(Duration.ofMinutes(5))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() throws Exception {
        areasWith(token(OTHER_SECRET, "devready", inTenMinutes()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenFromAnotherIssuer() throws Exception {
        areasWith(token(TestJwts.TEST_SECRET, "outro-emissor", inTenMinutes()))
                .andExpect(status().isUnauthorized());
    }
}
