package br.com.devready.infrastructure.evaluation.judge0;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.evaluation.dto.ExecutionRequest;
import br.com.devready.application.evaluation.exception.ExecutionUnavailableException;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.ExecutionOutcome;
import br.com.devready.domain.evaluation.ExecutionReport;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class Judge0CodeExecutorTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static WireMockServer judge0;

    private final List<Duration> sleeps = new ArrayList<>();

    @BeforeAll
    static void start() {
        judge0 = new WireMockServer(options().dynamicPort());
        judge0.start();
    }

    @AfterAll
    static void stop() {
        judge0.stop();
    }

    @BeforeEach
    void reset() {
        judge0.resetAll();
    }

    private Judge0CodeExecutor executor(String url, Duration timeout) {
        return new Judge0CodeExecutor(
                new Judge0Properties(
                        URI.create(url),
                        Duration.ofMillis(500),
                        timeout,
                        Map.of(
                                Language.JAVA, new Judge0Properties.LanguageSettings(62, 8_388_608),
                                Language.PYTHON,
                                        new Judge0Properties.LanguageSettings(71, 4_194_304),
                                Language.JAVASCRIPT,
                                        new Judge0Properties.LanguageSettings(63, 4_194_304))),
                sleeps::add);
    }

    private Judge0CodeExecutor executor() {
        return executor(judge0.baseUrl(), Duration.ofSeconds(60));
    }

    private static String rubyBase64(String text) {
        String flat = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < flat.length(); i += 60) {
            out.append(flat, i, Math.min(flat.length(), i + 60)).append('\n');
        }
        return out.toString();
    }

    private static String result(int status, String description, Map<String, String> fields) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", Map.of("id", status, "description", description));
        fields.forEach((name, value) -> body.put(name, rubyBase64(value)));
        return JSON.writeValueAsString(body);
    }

    private static void acceptsTheSubmission() {
        judge0.stubFor(
                post(urlPathEqualTo("/submissions"))
                        .withQueryParam("base64_encoded", equalTo("true"))
                        .withQueryParam("wait", equalTo("false"))
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"token\":\"t-1\"}")));
    }

    private static void answers(String body) {
        judge0.stubFor(get(urlPathEqualTo("/submissions/t-1")).willReturn(okJson(body)));
    }

    private static ExecutionRequest request(Language language) {
        return new ExecutionRequest(
                language, "class Main {} // ação", "[{\"position\":0,\"args\":[1]}]", 5000);
    }

    @Test
    void sendsTheProgramInBase64AndAsksUntilTheResultIsReady() {
        acceptsTheSubmission();
        String stdout =
                "#m#{\"position\":0,\"output\":\"ação\",\"error\":null,\"timeMs\":1}\n".repeat(3);
        judge0.stubFor(
                get(urlPathEqualTo("/submissions/t-1"))
                        .inScenario("execução")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(
                                okJson("{\"status\":{\"id\":2,\"description\":\"Processing\"}}"))
                        .willSetStateTo("pronto"));
        judge0.stubFor(
                get(urlPathEqualTo("/submissions/t-1"))
                        .inScenario("execução")
                        .whenScenarioStateIs("pronto")
                        .willReturn(okJson(result(3, "Accepted", Map.of("stdout", stdout)))));

        ExecutionReport report = executor().execute(request(Language.JAVA));

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.FINISHED);
        assertThat(report.stdout()).isEqualTo(stdout);
        assertThat(report.stderr()).isEmpty();
        assertThat(sleeps).containsExactly(Duration.ofMillis(500), Duration.ofMillis(500));
        String program =
                Base64.getEncoder()
                        .encodeToString("class Main {} // ação".getBytes(StandardCharsets.UTF_8));
        judge0.verify(
                postRequestedFor(urlPathEqualTo("/submissions"))
                        .withRequestBody(matchingJsonPath("$.language_id", equalTo("62")))
                        .withRequestBody(matchingJsonPath("$.source_code", equalTo(program)))
                        .withRequestBody(matchingJsonPath("$.cpu_time_limit", equalTo("5.0")))
                        .withRequestBody(matchingJsonPath("$.wall_time_limit", equalTo("10.0")))
                        .withRequestBody(matchingJsonPath("$.memory_limit", equalTo("8388608"))));
        judge0.verify(
                2,
                getRequestedFor(urlPathEqualTo("/submissions/t-1"))
                        .withQueryParam("base64_encoded", equalTo("true")));
    }

    @Test
    void pythonAndJavascriptUseTheirIdsAndMemory() {
        acceptsTheSubmission();
        answers(result(3, "Accepted", Map.of()));

        executor().execute(request(Language.PYTHON));
        executor().execute(request(Language.JAVASCRIPT));

        judge0.verify(
                postRequestedFor(urlPathEqualTo("/submissions"))
                        .withRequestBody(matchingJsonPath("$.language_id", equalTo("71")))
                        .withRequestBody(matchingJsonPath("$.memory_limit", equalTo("4194304"))));
        judge0.verify(
                postRequestedFor(urlPathEqualTo("/submissions"))
                        .withRequestBody(matchingJsonPath("$.language_id", equalTo("63"))));
    }

    @Test
    void aCompileErrorBringsTheCompilerOutput() {
        acceptsTheSubmission();
        answers(
                result(
                        6,
                        "Compilation Error",
                        Map.of("compile_output", "Main.java:3: error: ';' expected")));

        ExecutionReport report = executor().execute(request(Language.JAVA));

        assertThat(report.outcome()).isEqualTo(ExecutionOutcome.COMPILE_ERROR);
        assertThat(report.compileOutput()).isEqualTo("Main.java:3: error: ';' expected");
    }

    @Test
    void aTimeLimitAndACrashKeepWhatWasWritten() {
        acceptsTheSubmission();
        answers(result(5, "Time Limit Exceeded", Map.of("stdout", "#m#linha\n")));

        ExecutionReport timedOut = executor().execute(request(Language.PYTHON));

        judge0.resetAll();
        acceptsTheSubmission();
        answers(
                result(
                        11,
                        "Runtime Error (NZEC)",
                        Map.of(
                                "stderr", "Traceback: ZeroDivisionError",
                                "message", "Exited with error status 1")));

        ExecutionReport crashed = executor().execute(request(Language.PYTHON));

        assertThat(timedOut.outcome()).isEqualTo(ExecutionOutcome.TIME_LIMIT);
        assertThat(timedOut.stdout()).isEqualTo("#m#linha\n");
        assertThat(crashed.outcome()).isEqualTo(ExecutionOutcome.CRASHED);
        assertThat(crashed.stderr()).isEqualTo("Traceback: ZeroDivisionError");
        assertThat(crashed.message()).isEqualTo("Exited with error status 1");
    }

    @Test
    void anInternalErrorOfJudge0IsUnavailable() {
        acceptsTheSubmission();
        answers(result(13, "Internal Error", Map.of("message", "No such file or directory")));

        assertThatThrownBy(() -> executor().execute(request(Language.JAVA)))
                .isInstanceOf(ExecutionUnavailableException.class)
                .hasMessageContaining("13")
                .hasMessageContaining("Internal Error")
                .hasMessageContaining("No such file or directory");
    }

    @Test
    void anHttpErrorOrAnUnreadableAnswerIsUnavailable() {
        judge0.stubFor(
                post(urlPathEqualTo("/submissions")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> executor().execute(request(Language.JAVA)))
                .isInstanceOf(ExecutionUnavailableException.class)
                .hasMessageContaining("500");

        judge0.resetAll();
        acceptsTheSubmission();
        judge0.stubFor(
                get(urlPathEqualTo("/submissions/t-1"))
                        .willReturn(aResponse().withStatus(200).withBody("isto não é JSON")));

        assertThatThrownBy(() -> executor().execute(request(Language.JAVA)))
                .isInstanceOf(ExecutionUnavailableException.class)
                .hasMessageContaining("ilegível");
    }

    @Test
    void aJudge0ThatIsDownIsUnavailable() {
        assertThatThrownBy(
                        () ->
                                executor("http://127.0.0.1:1", Duration.ofSeconds(60))
                                        .execute(request(Language.JAVA)))
                .isInstanceOf(ExecutionUnavailableException.class)
                .hasMessageContaining("não respondeu");
    }

    @Test
    void givesUpAtTheCeiling() {
        acceptsTheSubmission();
        answers("{\"status\":{\"id\":2,\"description\":\"Processing\"}}");

        assertThatThrownBy(
                        () ->
                                executor(judge0.baseUrl(), Duration.ofSeconds(2))
                                        .execute(request(Language.JAVA)))
                .isInstanceOf(ExecutionUnavailableException.class)
                .hasMessageContaining("2 s");
        judge0.verify(4, getRequestedFor(urlPathEqualTo("/submissions/t-1")));
        assertThat(sleeps).hasSize(4);
    }

    @Test
    void theDefaultsAreHalfASecondAndOneMinute() {
        Judge0Properties properties =
                new Judge0Properties(URI.create("http://localhost:2358"), null, null, null);

        assertThat(properties.pollInterval()).isEqualTo(Duration.ofMillis(500));
        assertThat(properties.pollTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.maxPolls()).isEqualTo(120);
        assertThatThrownBy(() -> properties.settingsOf(Language.JAVA))
                .isInstanceOf(IllegalStateException.class);
    }
}
