package br.com.devready.infrastructure.evaluation.harness;

import br.com.devready.application.evaluation.port.HarnessBuilder;
import br.com.devready.domain.catalog.Language;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TemplateHarnessBuilder implements HarnessBuilder {

    static final String MARKER_PLACEHOLDER = "__DEVREADY_MARKER__";
    static final String FUNCTION_PLACEHOLDER = "__DEVREADY_FUNCTION__";

    private static final Pattern FUNCTION = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern MARKER = Pattern.compile("[A-Za-z0-9#_-]{8,64}");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^[ \\t]*package\\s+[\\w.]+\\s*;");
    private static final Pattern PUBLIC_TOP_LEVEL =
            Pattern.compile(
                    "(?m)^public\\s+(?=(?:(?:final|abstract|strictfp)\\s+)*(?:class|interface|enum)\\b)");

    private static final Map<Language, String> FILES =
            Map.of(
                    Language.JAVA, "/harness/java/Main.java.tpl",
                    Language.PYTHON, "/harness/python/main.py.tpl",
                    Language.JAVASCRIPT, "/harness/javascript/main.js.tpl");

    private final Map<Language, String> templates = new EnumMap<>(Language.class);

    public TemplateHarnessBuilder() {
        FILES.forEach((language, file) -> templates.put(language, read(file)));
    }

    @Override
    public String build(Language language, String sourceCode, String functionName, String marker) {
        if (functionName == null || !FUNCTION.matcher(functionName).matches()) {
            throw new IllegalArgumentException("Nome de função inválido: " + functionName);
        }
        if (marker == null || !MARKER.matcher(marker).matches()) {
            throw new IllegalArgumentException("Marcador do harness inválido.");
        }
        String harness =
                templates
                        .get(language)
                        .replace(MARKER_PLACEHOLDER, marker)
                        .replace(FUNCTION_PLACEHOLDER, functionName);
        String code = sourceCode.replace("\r\n", "\n").replace('\r', '\n');
        if (language == Language.JAVA) {
            code = javaSource(code);
        }
        String program = code + (code.endsWith("\n") ? "\n" : "\n\n") + harness;
        return language == Language.JAVA ? asciiOnly(program) : program;
    }

    private static String javaSource(String code) {
        String withoutPackage = PACKAGE.matcher(code).replaceAll("");
        return PUBLIC_TOP_LEVEL.matcher(withoutPackage).replaceAll("");
    }

    private static String asciiOnly(String source) {
        StringBuilder out = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c < 0x80) {
                out.append(c);
            } else {
                out.append(String.format("\\u%04x", (int) c));
            }
        }
        return out.toString();
    }

    private static String read(String file) {
        try (InputStream in = TemplateHarnessBuilder.class.getResourceAsStream(file)) {
            if (in == null) {
                throw new IllegalStateException("Template do harness não encontrado: " + file);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Template do harness ilegível: " + file, e);
        }
    }
}
