package br.com.devready.domain.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.TestCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HarnessInputTest {

    @Test
    void writesTheCasesInPositionOrderWithTheirArguments() {
        List<TestCase> cases =
                List.of(
                        new TestCase(UUID.randomUUID(), 3, "[[3, 3], 6]", "[0,1]", false),
                        new TestCase(UUID.randomUUID(), 0, "[[2,7,11,15],9]", "[0,1]", true),
                        new TestCase(UUID.randomUUID(), 1, "[\"ação\"]", "1", true));

        assertThat(HarnessInput.of(cases))
                .isEqualTo(
                        "[{\"position\":0,\"args\":[[2,7,11,15],9]},"
                                + "{\"position\":1,\"args\":[\"ação\"]},"
                                + "{\"position\":3,\"args\":[[3,3],6]}]");
    }

    @Test
    void argumentsMustBeAList() {
        List<TestCase> cases = List.of(new TestCase(UUID.randomUUID(), 0, "9", "1", true));

        assertThatThrownBy(() -> HarnessInput.of(cases))
                .isInstanceOf(EvaluationException.class)
                .hasMessageContaining("posição 0");
    }
}
