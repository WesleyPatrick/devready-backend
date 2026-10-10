package br.com.devready.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.api.HttpIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class ErrorResponsesTest extends HttpIntegrationTest {

    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

    private String registerCandidate() {
        String body =
                http.post()
                        .uri("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                "{\"email\":\"e-"
                                        + UUID.randomUUID()
                                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}")
                        .retrieve()
                        .body(String.class);
        return JsonPath.read(body, "$.accessToken");
    }

    private static void assertProblem(ResponseEntity<String> response, HttpStatus status) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(PROBLEM)).isTrue();
        assertThat((Integer) JsonPath.read(response.getBody(), "$.status"))
                .isEqualTo(status.value());
        assertThat(response.getBody()).doesNotContain("\"properties\"");
    }

    @Test
    void malformedJsonOnPublicRouteIsBadRequest() {
        ResponseEntity<String> response =
                http.post()
                        .uri("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"email\":")
                        .retrieve()
                        .toEntity(String.class);

        assertProblem(response, HttpStatus.BAD_REQUEST);
        assertThat((String) JsonPath.read(response.getBody(), "$.title"))
                .isEqualTo("Requisição inválida");
    }

    @Test
    void unknownRouteIsNotFound() {
        ResponseEntity<String> response =
                http.get()
                        .uri("/api/v1/nao-existe")
                        .header("Authorization", "Bearer " + registerCandidate())
                        .retrieve()
                        .toEntity(String.class);

        assertProblem(response, HttpStatus.NOT_FOUND);
        assertThat((String) JsonPath.read(response.getBody(), "$.title"))
                .isEqualTo("Não encontrado");
    }

    @Test
    void wrongMethodOnPublicRouteIsMethodNotAllowed() {
        ResponseEntity<String> response =
                http.put()
                        .uri("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}")
                        .retrieve()
                        .toEntity(String.class);

        assertProblem(response, HttpStatus.METHOD_NOT_ALLOWED);
        assertThat((String) JsonPath.read(response.getBody(), "$.title"))
                .isEqualTo("Método não permitido");
    }

    @Test
    void missingTokenIsUnauthorizedProblem() {
        ResponseEntity<String> response =
                http.get().uri("/api/v1/auth/me").retrieve().toEntity(String.class);

        assertProblem(response, HttpStatus.UNAUTHORIZED);
        assertThat((String) JsonPath.read(response.getBody(), "$.title"))
                .isEqualTo("Não autenticado");
    }

    @Test
    void candidateOnAdminRouteIsForbiddenProblem() {
        ResponseEntity<String> response =
                http.get()
                        .uri("/api/v1/admin/anything")
                        .header("Authorization", "Bearer " + registerCandidate())
                        .retrieve()
                        .toEntity(String.class);

        assertProblem(response, HttpStatus.FORBIDDEN);
        assertThat((String) JsonPath.read(response.getBody(), "$.title"))
                .isEqualTo("Sem permissão");
    }

    @Test
    void simultaneousRegistrationsWithTheSameEmailNeverFailWithServerError() throws Exception {
        String body =
                "{\"email\":\"c-"
                        + UUID.randomUUID()
                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}";
        List<Callable<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            calls.add(
                    () ->
                            http.post()
                                    .uri("/api/v1/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(body)
                                    .retrieve()
                                    .toBodilessEntity()
                                    .getStatusCode()
                                    .value());
        }
        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(6)) {
            for (Future<Integer> result : pool.invokeAll(calls)) {
                statuses.add(result.get());
            }
        }

        assertThat(statuses).containsOnly(201, 409);
        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
    }
}
