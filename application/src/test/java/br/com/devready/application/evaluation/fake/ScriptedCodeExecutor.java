package br.com.devready.application.evaluation.fake;

import br.com.devready.application.evaluation.dto.ExecutionRequest;
import br.com.devready.application.evaluation.exception.ExecutionUnavailableException;
import br.com.devready.application.evaluation.port.CodeExecutor;
import br.com.devready.domain.evaluation.ExecutionOutcome;
import br.com.devready.domain.evaluation.ExecutionReport;
import br.com.devready.domain.evaluation.Json;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ScriptedCodeExecutor implements CodeExecutor {

    private final Deque<Function<ExecutionRequest, ExecutionReport>> next = new ArrayDeque<>();
    private final List<ExecutionRequest> calls = new ArrayList<>();
    private Function<List<?>, Object> answer = args -> args.get(0);
    private Runnable during = () -> {};

    @Override
    public ExecutionReport execute(ExecutionRequest request) {
        calls.add(request);
        Runnable action = during;
        during = () -> {};
        action.run();
        Function<ExecutionRequest, ExecutionReport> step = next.poll();
        if (step == null) {
            return new ExecutionReport(
                    ExecutionOutcome.FINISHED, lines(request, Integer.MAX_VALUE), "", null, null);
        }
        return step.apply(request);
    }

    public void answers(Function<List<?>, Object> answer) {
        this.answer = answer;
    }

    public void thenUnavailable(int times) {
        for (int i = 0; i < times; i++) {
            next.add(
                    request -> {
                        throw new ExecutionUnavailableException("Judge0 fora do ar");
                    });
        }
    }

    public void thenReport(ExecutionReport report) {
        next.add(request -> report);
    }

    public void thenTimeLimitAfter(int answered) {
        next.add(
                request ->
                        new ExecutionReport(
                                ExecutionOutcome.TIME_LIMIT,
                                lines(request, answered),
                                "",
                                null,
                                null));
    }

    public void during(Runnable action) {
        this.during = action;
    }

    public List<ExecutionRequest> calls() {
        return List.copyOf(calls);
    }

    public static String markerOf(ExecutionRequest request) {
        return request.program().lines().findFirst().orElseThrow();
    }

    private String lines(ExecutionRequest request, int limit) {
        StringBuilder out = new StringBuilder();
        int count = 0;
        for (Object item : (List<?>) Json.parse(request.stdin())) {
            if (count++ >= limit) {
                break;
            }
            Map<?, ?> testCase = (Map<?, ?>) item;
            String output = "null";
            String error = "null";
            try {
                output = Json.write(answer.apply((List<?>) testCase.get("args")));
            } catch (RuntimeException e) {
                error = Json.write(e.getMessage());
            }
            out.append(markerOf(request))
                    .append("{\"position\":")
                    .append(Json.write(testCase.get("position")))
                    .append(",\"output\":")
                    .append(output)
                    .append(",\"error\":")
                    .append(error)
                    .append(",\"timeMs\":1}\n");
        }
        return out.toString();
    }
}
