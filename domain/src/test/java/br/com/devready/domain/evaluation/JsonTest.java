package br.com.devready.domain.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void readsObjectsListsTextsNumbersBooleansAndNull() {
        Object value =
                Json.parse(
                        " {\"position\": 2, \"output\": [1, -2.50, 3e2, true, false, null],"
                                + " \"error\": \"x\"} ");

        assertThat(value).isInstanceOf(Map.class);
        Map<?, ?> fields = (Map<?, ?>) value;
        assertThat(new ArrayList<Object>(fields.keySet()))
                .containsExactly("position", "output", "error");
        assertThat(fields.get("position")).isEqualTo(new BigDecimal("2"));
        assertThat(new ArrayList<Object>((List<?>) fields.get("output")))
                .containsExactly(
                        new BigDecimal("1"),
                        new BigDecimal("-2.50"),
                        new BigDecimal("3e2"),
                        true,
                        false,
                        null);
        assertThat(fields.get("error")).isEqualTo("x");
    }

    @Test
    void readsEscapesAndUnicodeLikeThePythonWrites() {
        assertThat(Json.parse("\"a\\u00e7\\u00e3o\"")).isEqualTo("ação");
        assertThat(Json.parse("\"ação\"")).isEqualTo("ação");
        assertThat(Json.parse("\"aspas \\\" barra \\\\ \\/ \\n\\t\\r\\b\\f\""))
                .isEqualTo("aspas \" barra \\ / \n\t\r\b\f");
    }

    @Test
    void refusesWhatIsNotJson() {
        for (String broken :
                List.of(
                        "",
                        "[1,",
                        "{\"a\" 1}",
                        "[1] 2",
                        "tru",
                        "01",
                        "\"sem fim",
                        "\"\\x\"",
                        "\"\\u12g4\"",
                        "\"\\u+123\"",
                        "{1: 2}",
                        "1e99999999999")) {
            assertThatThrownBy(() -> Json.parse(broken))
                    .as(broken)
                    .isInstanceOf(MalformedJsonException.class);
        }
        assertThatThrownBy(() -> Json.parse(null)).isInstanceOf(MalformedJsonException.class);
    }

    @Test
    void refusesNestingDeeperThanTheLimit() {
        String deep = "[".repeat(600) + "]".repeat(600);

        assertThatThrownBy(() -> Json.parse(deep))
                .isInstanceOf(MalformedJsonException.class)
                .hasMessageContaining("aninhado");
    }

    @Test
    void writesCompactWithNumbersByValue() {
        assertThat(Json.write(Json.parse("[1.0, 2.50, 1e2, -0.0, 0.000001]")))
                .isEqualTo("[1,2.5,100,0,0.000001]");
        assertThat(Json.write(Json.parse("{\"b\": [true, null], \"a\": \"x\"}")))
                .isEqualTo("{\"b\":[true,null],\"a\":\"x\"}");
        assertThat(Json.write(List.of(1, 2L))).isEqualTo("[1,2]");
        assertThat(Json.write(null)).isEqualTo("null");
    }

    @Test
    void writesTextsWithEscapesAndKeepsAccents() {
        assertThat(Json.write("ação \"x\" \\ \n\u0001"))
                .isEqualTo("\"ação \\\"x\\\" \\\\ \\n\\u0001\"");
    }

    @Test
    void aHugeExponentIsWrittenWithoutExpandingIt() {
        assertThat(Json.write(new BigDecimal("1e1000"))).isEqualTo("1E+1000");
    }

    @Test
    void listsAndObjectsReadAreReadOnly() {
        List<?> list = (List<?>) Json.parse("[1]");

        assertThatThrownBy(() -> list.remove(0)).isInstanceOf(UnsupportedOperationException.class);
        assertThat((List<?>) Json.parse("[ ]")).isEmpty();
    }
}
