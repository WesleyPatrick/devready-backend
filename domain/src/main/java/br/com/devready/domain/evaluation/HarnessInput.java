package br.com.devready.domain.evaluation;

import br.com.devready.domain.catalog.TestCase;
import java.util.Comparator;
import java.util.List;

public final class HarnessInput {

    private HarnessInput() {}

    public static String of(List<TestCase> cases) {
        StringBuilder json = new StringBuilder("[");
        cases.stream()
                .sorted(Comparator.comparingInt(TestCase::position))
                .forEach(
                        testCase -> {
                            if (json.length() > 1) {
                                json.append(',');
                            }
                            Object args = Json.parse(testCase.argsJson());
                            if (!(args instanceof List<?>)) {
                                throw new EvaluationException(
                                        "Os argumentos do caso da posição "
                                                + testCase.position()
                                                + " não são uma lista.");
                            }
                            json.append("{\"position\":")
                                    .append(testCase.position())
                                    .append(",\"args\":")
                                    .append(Json.write(args))
                                    .append('}');
                        });
        return json.append(']').toString();
    }
}
