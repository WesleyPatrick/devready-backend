package br.com.devready.domain.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.catalog.ComparisonMode;
import org.junit.jupiter.api.Test;

class OutputComparisonTest {

    private static boolean matches(String expected, String actual, ComparisonMode mode) {
        return OutputComparison.matches(Json.parse(expected), Json.parse(actual), mode);
    }

    @Test
    void exactComparesTheStructureAndNumbersByValue() {
        assertThat(matches("[0, 1]", "[0, 1]", ComparisonMode.EXACT)).isTrue();
        assertThat(matches("[0, 1]", "[0.0, 1.00]", ComparisonMode.EXACT)).isTrue();
        assertThat(matches("[0, 1]", "[1, 0]", ComparisonMode.EXACT)).isFalse();
        assertThat(matches("[[1, 2], [3]]", "[[1, 2], [3]]", ComparisonMode.EXACT)).isTrue();
        assertThat(matches("[[1, 2], [3]]", "[[1, 2], [3, 4]]", ComparisonMode.EXACT)).isFalse();
        assertThat(matches("{\"a\": 1}", "{\"a\": 1.0}", ComparisonMode.EXACT)).isTrue();
        assertThat(matches("{\"a\": 1}", "{\"a\": 1, \"b\": 2}", ComparisonMode.EXACT)).isFalse();
    }

    @Test
    void textsWithAccentsMatchHoweverTheyWereEscaped() {
        assertThat(matches("\"ação\"", "\"a\\u00e7\\u00e3o\"", ComparisonMode.EXACT)).isTrue();
        assertThat(matches("\"ação\"", "\"acao\"", ComparisonMode.EXACT)).isFalse();
    }

    @Test
    void typesNeverMix() {
        assertThat(matches("1", "\"1\"", ComparisonMode.EXACT)).isFalse();
        assertThat(matches("true", "1", ComparisonMode.EXACT)).isFalse();
        assertThat(matches("null", "[]", ComparisonMode.EXACT)).isFalse();
        assertThat(matches("null", "null", ComparisonMode.EXACT)).isTrue();
    }

    @Test
    void unorderedTreatsTheTopLevelAsAMultiset() {
        assertThat(matches("[0, 1]", "[1, 0]", ComparisonMode.UNORDERED)).isTrue();
        assertThat(matches("[1, 1, 2]", "[2, 1, 1]", ComparisonMode.UNORDERED)).isTrue();
        assertThat(matches("[1, 1, 2]", "[1, 2, 2]", ComparisonMode.UNORDERED)).isFalse();
        assertThat(matches("[1, 2]", "[1, 2, 2]", ComparisonMode.UNORDERED)).isFalse();
        assertThat(matches("[[1, 2], [3]]", "[[3], [1, 2]]", ComparisonMode.UNORDERED)).isTrue();
        assertThat(matches("[[1, 2], [3]]", "[[3], [2, 1]]", ComparisonMode.UNORDERED)).isFalse();
        assertThat(matches("5", "5.0", ComparisonMode.UNORDERED)).isTrue();
    }

    @Test
    void floatToleranceAcceptsUpToOneMillionth() {
        assertThat(matches("[0.1, 2]", "[0.1000005, 2]", ComparisonMode.FLOAT_TOLERANCE)).isTrue();
        assertThat(matches("0.1", "0.100002", ComparisonMode.FLOAT_TOLERANCE)).isFalse();
        assertThat(matches("\"a\"", "\"a\"", ComparisonMode.FLOAT_TOLERANCE)).isTrue();
        assertThat(matches("1e400", "1e400", ComparisonMode.FLOAT_TOLERANCE)).isTrue();
        assertThat(matches("1e400", "2e400", ComparisonMode.FLOAT_TOLERANCE)).isFalse();
    }
}
