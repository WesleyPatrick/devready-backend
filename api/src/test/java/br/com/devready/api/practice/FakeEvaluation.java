package br.com.devready.api.practice;

import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.evaluation.dto.ExecutionRequest;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.evaluation.usecase.ProcessSubmissionUseCase;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.domain.evaluation.ExecutionOutcome;
import br.com.devready.domain.evaluation.ExecutionReport;
import br.com.devready.domain.evaluation.Json;
import br.com.devready.infrastructure.evaluation.messaging.EvaluationMessaging;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class FakeEvaluation {

    public static final String SOLVES = "SOMA_CERTA";
    public static final String BREAKS_THE_BUILD = "NAO_COMPILA";
    public static final String CRASHES = "ERRO_EXECUCAO";
    public static final String CRASH_OUTPUT = "stderr com o argumento de um caso";

    @Bean
    ProcessSubmissionUseCase fakeProcessSubmission(
            SubmissionRepository submissions,
            ChallengeRepository challenges,
            IdGenerator ids,
            Clock clock) {
        return new ProcessSubmissionUseCase(
                submissions,
                challenges,
                (language, code, function, marker) -> marker + "\n" + code,
                FakeEvaluation::execute,
                ids,
                clock,
                duration -> {});
    }

    @Bean
    FakeWorker fakeWorker(ProcessSubmissionUseCase fakeProcessSubmission) {
        return new FakeWorker(fakeProcessSubmission);
    }

    public static class FakeWorker {
        private final ProcessSubmissionUseCase process;

        FakeWorker(ProcessSubmissionUseCase process) {
            this.process = process;
        }

        @RabbitListener(queues = EvaluationMessaging.QUEUE)
        public void onMessage(String id) {
            process.execute(UUID.fromString(id));
        }
    }

    static ExecutionReport execute(ExecutionRequest request) {
        String marker = request.program().lines().findFirst().orElseThrow();
        String code = request.program().substring(marker.length());
        if (code.contains(BREAKS_THE_BUILD)) {
            return new ExecutionReport(
                    ExecutionOutcome.COMPILE_ERROR,
                    "",
                    "",
                    "Main.java:3: error: ';' expected",
                    null);
        }
        if (code.contains(CRASHES)) {
            return new ExecutionReport(ExecutionOutcome.FINISHED, "", CRASH_OUTPUT, null, null);
        }
        StringBuilder stdout = new StringBuilder();
        for (Object item : (List<?>) Json.parse(request.stdin())) {
            Map<?, ?> testCase = (Map<?, ?>) item;
            List<?> args = (List<?>) testCase.get("args");
            Object output =
                    code.contains(SOLVES) && isTwoSum(args)
                            ? twoSum((List<?>) args.get(0), (BigDecimal) args.get(1))
                            : List.of();
            stdout.append(marker)
                    .append("{\"position\":")
                    .append(Json.write(testCase.get("position")))
                    .append(",\"output\":")
                    .append(Json.write(output))
                    .append(",\"error\":null,\"timeMs\":1}\n");
        }
        return new ExecutionReport(ExecutionOutcome.FINISHED, stdout.toString(), "", null, null);
    }

    private static boolean isTwoSum(List<?> args) {
        return args.size() == 2 && args.get(0) instanceof List && args.get(1) instanceof BigDecimal;
    }

    private static List<Integer> twoSum(List<?> nums, BigDecimal target) {
        for (int i = 0; i < nums.size(); i++) {
            for (int j = i + 1; j < nums.size(); j++) {
                BigDecimal sum = ((BigDecimal) nums.get(i)).add((BigDecimal) nums.get(j));
                if (sum.compareTo(target) == 0) {
                    return List.of(i, j);
                }
            }
        }
        return List.of();
    }
}
