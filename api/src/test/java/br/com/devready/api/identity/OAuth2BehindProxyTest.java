package br.com.devready.api.identity;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.api.HttpIntegrationTest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class OAuth2BehindProxyTest extends HttpIntegrationTest {

    @Test
    void redirectUriUsesHttpsWhenTheProxySaysSo() {
        ResponseEntity<Void> response =
                http.get()
                        .uri("/oauth2/authorization/google")
                        .header("X-Forwarded-Proto", "https")
                        .retrieve()
                        .toBodilessEntity();

        assertThat(response.getStatusCode().is3xxRedirection()).isTrue();
        String location =
                URLDecoder.decode(
                        response.getHeaders().getLocation().toString(), StandardCharsets.UTF_8);
        assertThat(location).contains("redirect_uri=https://localhost/login/oauth2/code/google");
    }
}
