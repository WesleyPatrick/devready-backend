package br.com.devready.api;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class HttpIntegrationTest {

    @LocalServerPort private int port;

    protected RestClient http;

    @BeforeEach
    void createClient() {
        http =
                RestClient.builder()
                        .baseUrl(baseUrl())
                        .requestFactory(new JdkClientHttpRequestFactory())
                        .defaultStatusHandler(status -> true, (request, response) -> {})
                        .build();
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
