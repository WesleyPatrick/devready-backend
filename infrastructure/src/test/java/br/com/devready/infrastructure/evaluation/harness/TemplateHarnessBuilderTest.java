package br.com.devready.infrastructure.evaluation.harness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.Language;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemplateHarnessBuilderTest {

    private static final String MARKER = "#devready-0123456789abcdef#";

    private final TemplateHarnessBuilder builder = new TemplateHarnessBuilder();

    @Test
    void javaKeepsTheImportsOfThePersonAtTheTopAndEveryLineInPlace() {
        String code =
                "import java.util.HashMap;\n"
                        + "import java.util.Map;\n"
                        + "\n"
                        + "class Solution {\n"
                        + "    public int[] twoSum(int[] nums, int target) {\n"
                        + "        Map<Integer, Integer> seen = new HashMap<>();\n"
                        + "        return new int[0];\n"
                        + "    }\n"
                        + "}\n";

        String program = builder.build(Language.JAVA, code, "twoSum", MARKER);

        List<String> lines = program.lines().toList();
        assertThat(lines.subList(0, 9)).containsExactlyElementsOf(code.lines().toList());
        assertThat(program.indexOf("import java.util.Map;"))
                .isLessThan(program.indexOf("class Solution"));
        assertThat(program.indexOf("class Solution"))
                .isLessThan(program.indexOf("public class Main"));
        assertThat(program)
                .contains("FUNCTION = \"twoSum\"")
                .contains("MARKER = \"" + MARKER + "\"")
                .doesNotContain("__DEVREADY_");
        assertThat(program.substring(program.indexOf("public class Main")))
                .doesNotContain("\nimport ");
    }

    @Test
    void javaDropsThePackageAndThePublicOfTopLevelTypesKeepingTheLineCount() {
        String code =
                "package app;\n"
                        + "public class Solution {\n"
                        + "    public int f(int x) { return x; }\n"
                        + "}\n";

        List<String> lines = builder.build(Language.JAVA, code, "f", MARKER).lines().toList();

        assertThat(lines.get(0)).isBlank();
        assertThat(lines.get(1)).isEqualTo("class Solution {");
        assertThat(lines.get(2)).isEqualTo("    public int f(int x) { return x; }");
    }

    @Test
    void javaGoesOutInAsciiWithUnicodeEscapes() {
        String code =
                "class Solution {\n"
                        + "    // sua solução\n"
                        + "    public String f() { return \"ação\"; }\n"
                        + "}\n";

        String program = builder.build(Language.JAVA, code, "f", MARKER);

        assertThat(program.chars().allMatch(c -> c < 128)).isTrue();
        assertThat(program)
                .contains("// sua solu\\u00e7\\u00e3o")
                .contains("return \"a\\u00e7\\u00e3o\";");
    }

    @Test
    void pythonCallsTheFunctionByNameAfterTheCodeOfThePerson() {
        String code =
                "class Solution:\n"
                        + "    def two_sum(self, nums, target):\n"
                        + "        return []  # ação\n";

        String program = builder.build(Language.PYTHON, code, "two_sum", MARKER);

        assertThat(program).startsWith(code);
        assertThat(program)
                .contains("getattr(solution, \"two_sum\")")
                .contains("\"" + MARKER + "\" + line")
                .doesNotContain("__DEVREADY_");
    }

    @Test
    void javascriptGuardsTheHarnessWithASemicolon() {
        String code = "class Solution {\n  twoSum(nums, target) {\n    return []\n  }\n}";

        String program = builder.build(Language.JAVASCRIPT, code, "twoSum", MARKER);

        assertThat(program).startsWith(code + "\n\n;(function");
        assertThat(program)
                .contains("solution['twoSum']")
                .contains("'" + MARKER + "' + line")
                .doesNotContain("__DEVREADY_");
    }

    @Test
    void theCodeOfThePersonCannotReachThePlaceholders() {
        String code =
                "class Solution:\n"
                        + "    def f(self):\n"
                        + "        return '__DEVREADY_MARKER__'\n";

        String program = builder.build(Language.PYTHON, code, "f", MARKER);

        assertThat(program).contains("return '__DEVREADY_MARKER__'");
        assertThat(program).contains("\"" + MARKER + "\" + line");
    }

    @Test
    void windowsLineBreaksBecomeUnix() {
        String code = "class Solution:\r\n    def f(self):\r\n        return 1\r\n";

        String program = builder.build(Language.PYTHON, code, "f", MARKER);

        assertThat(program)
                .startsWith("class Solution:\n    def f(self):\n        return 1\n")
                .doesNotContain("\r");
    }

    @Test
    void refusesAFunctionNameOrAMarkerThatCouldBreakTheProgram() {
        assertThatThrownBy(
                        () ->
                                builder.build(
                                        Language.JAVA,
                                        "class Solution {}",
                                        "f\"); System.exit(0",
                                        MARKER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> builder.build(Language.JAVA, "class Solution {}", "f", "\"; quebra"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
