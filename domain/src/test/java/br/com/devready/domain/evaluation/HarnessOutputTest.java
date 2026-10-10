package br.com.devready.domain.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HarnessOutputTest {

    private static final String MARKER = "#devready-abc123#";

    @Test
    void readsOneMarkedLinePerCase() {
        String stdout =
                MARKER
                        + "{\"position\":0,\"output\":[0,1],\"error\":null,\"timeMs\":3}\n"
                        + MARKER
                        + "{\"position\":1,\"output\":null,\"error\":\"java.lang.ArithmeticException: / by zero\",\"timeMs\":1}\r\n";

        Map<Integer, HarnessLine> lines = HarnessOutput.parse(stdout, MARKER);

        assertThat(lines).containsOnlyKeys(0, 1);
        assertThat(lines.get(0).output()).isEqualTo(List.of(BigDecimal.ZERO, BigDecimal.ONE));
        assertThat(lines.get(0).error()).isNull();
        assertThat(lines.get(0).timeMs()).isEqualTo(3);
        assertThat(lines.get(1).error()).isEqualTo("java.lang.ArithmeticException: / by zero");
    }

    @Test
    void ignoresLinesWithoutTheMarkerAndUnreadableOnes() {
        String stdout =
                "{\"position\":0,\"output\":1,\"error\":null,\"timeMs\":1}\n"
                        + "print da pessoa\n"
                        + MARKER
                        + "{\"position\":1,\"output\":\n"
                        + MARKER
                        + "[1,2]\n"
                        + MARKER
                        + "{\"output\":1}\n"
                        + MARKER
                        + "{\"position\":-1,\"output\":1}\n"
                        + MARKER
                        + "{\"position\":1.5,\"output\":1}\n";

        assertThat(HarnessOutput.parse(stdout, MARKER)).isEmpty();
    }

    @Test
    void aMarkedLineInTheMiddleOfAPrintCountsAndTheFirstLineOfAPositionWins() {
        String stdout =
                "sem quebra de linha"
                        + MARKER
                        + "{\"position\":2,\"output\":\"a\",\"error\":\"\",\"timeMs\":-5}\n"
                        + MARKER
                        + "{\"position\":2,\"output\":\"b\",\"error\":null,\"timeMs\":1}\n";

        HarnessLine line = HarnessOutput.parse(stdout, MARKER).get(2);

        assertThat(line.output()).isEqualTo("a");
        assertThat(line.error()).isNull();
        assertThat(line.timeMs()).isZero();
    }

    @Test
    void emptyOutputHasNoLinesAndTheMarkerIsRequired() {
        assertThat(HarnessOutput.parse(null, MARKER)).isEmpty();
        assertThat(HarnessOutput.parse("", MARKER)).isEmpty();
        assertThatThrownBy(() -> HarnessOutput.parse("x", " "))
                .isInstanceOf(EvaluationException.class);
    }
}
