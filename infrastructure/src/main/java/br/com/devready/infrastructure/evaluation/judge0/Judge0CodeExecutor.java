package br.com.devready.infrastructure.evaluation.judge0;

import br.com.devready.application.evaluation.dto.ExecutionRequest;
import br.com.devready.application.evaluation.exception.ExecutionUnavailableException;
import br.com.devready.application.evaluation.port.CodeExecutor;
import br.com.devready.application.shared.Sleeper;
import br.com.devready.domain.evaluation.ExecutionOutcome;
import br.com.devready.domain.evaluation.ExecutionReport;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public class Judge0CodeExecutor implements CodeExecutor {

    private static final String FIELDS = "status,stdout,stderr,compile_output,message";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final Judge0Properties properties;
    private final Sleeper sleeper;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public Judge0CodeExecutor(Judge0Properties properties, Sleeper sleeper) {
        this(
                properties,
                sleeper,
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(3))
                        .build());
    }

    Judge0CodeExecutor(Judge0Properties properties, Sleeper sleeper, HttpClient http) {
        this.properties = properties;
        this.sleeper = sleeper;
        this.http = http;
    }

    @Override
    public ExecutionReport execute(ExecutionRequest request) {
        String token = submit(request);
        String path =
                "/submissions/"
                        + URLEncoder.encode(token, StandardCharsets.UTF_8)
                        + "?base64_encoded=true&fields="
                        + FIELDS;
        for (int poll = 0; poll < properties.maxPolls(); poll++) {
            sleeper.sleep(properties.pollInterval());
            JsonNode result =
                    send(
                            HttpRequest.newBuilder(endpoint(path))
                                    .timeout(REQUEST_TIMEOUT)
                                    .GET()
                                    .build(),
                            "ao devolver o resultado");
            int status = result.path("status").path("id").asInt(0);
            if (status > 2) {
                return report(status, result);
            }
        }
        throw new ExecutionUnavailableException(
                "O Judge0 não terminou o envio em " + properties.pollTimeout().toSeconds() + " s.");
    }

    private String submit(ExecutionRequest request) {
        Judge0Properties.LanguageSettings language = properties.settingsOf(request.language());
        double cpuSeconds = request.timeLimitMs() / 1000.0;
        ObjectNode body = json.createObjectNode();
        body.put("source_code", encode(request.program()));
        body.put("language_id", language.id());
        body.put("stdin", encode(request.stdin()));
        body.put("cpu_time_limit", cpuSeconds);
        body.put("wall_time_limit", cpuSeconds * 2);
        body.put("memory_limit", language.memoryLimitKb());
        JsonNode created =
                send(
                        HttpRequest.newBuilder(
                                        endpoint("/submissions?base64_encoded=true&wait=false"))
                                .timeout(REQUEST_TIMEOUT)
                                .header("Content-Type", "application/json")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                json.writeValueAsString(body)))
                                .build(),
                        "ao receber o envio");
        JsonNode token = created.path("token");
        if (!token.isString() || token.stringValue().isBlank()) {
            throw new ExecutionUnavailableException("O Judge0 não devolveu o token do envio.");
        }
        return token.stringValue();
    }

    private JsonNode send(HttpRequest request, String step) {
        HttpResponse<String> response;
        try {
            response =
                    http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ExecutionUnavailableException(
                    "O Judge0 não respondeu " + step + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExecutionUnavailableException("A espera pelo Judge0 foi interrompida.", e);
        }
        if (response.statusCode() / 100 != 2) {
            throw new ExecutionUnavailableException(
                    "O Judge0 respondeu " + response.statusCode() + " " + step + ".");
        }
        try {
            return json.readTree(response.body());
        } catch (JacksonException e) {
            throw new ExecutionUnavailableException("Resposta ilegível do Judge0 " + step + ".", e);
        }
    }

    private ExecutionReport report(int status, JsonNode result) {
        String stdout = decode(result.path("stdout"));
        String stderr = decode(result.path("stderr"));
        String compileOutput = decode(result.path("compile_output"));
        String message = decode(result.path("message"));
        return switch (status) {
            case 3, 4 ->
                    new ExecutionReport(ExecutionOutcome.FINISHED, stdout, stderr, null, message);
            case 5 ->
                    new ExecutionReport(ExecutionOutcome.TIME_LIMIT, stdout, stderr, null, message);
            case 6 ->
                    new ExecutionReport(
                            ExecutionOutcome.COMPILE_ERROR, stdout, stderr, compileOutput, message);
            case 7, 8, 9, 10, 11, 12 ->
                    new ExecutionReport(ExecutionOutcome.CRASHED, stdout, stderr, null, message);
            default ->
                    throw new ExecutionUnavailableException(
                            "O Judge0 não conseguiu executar o envio (situação "
                                    + status
                                    + ", "
                                    + result.path("status").path("description").asString("")
                                    + "): "
                                    + (message == null ? "sem mensagem" : message));
        };
    }

    private URI endpoint(String path) {
        String base = properties.url().toString();
        return URI.create(
                (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + path);
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(JsonNode node) {
        if (!node.isString()) {
            return null;
        }
        try {
            return new String(
                    Base64.getMimeDecoder().decode(node.stringValue()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ExecutionUnavailableException("Base64 ilegível na resposta do Judge0.", e);
        }
    }
}
