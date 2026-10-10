package br.com.devready.domain.evaluation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class Json {

    static final int MAX_DEPTH = 512;

    private static final Pattern NUMBER =
            Pattern.compile("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?");

    private final String text;
    private int at;

    private Json(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        if (text == null) {
            throw new MalformedJsonException("JSON ausente.");
        }
        Json reader = new Json(text);
        reader.skipSpaces();
        Object value = reader.value(0);
        reader.skipSpaces();
        if (reader.at != text.length()) {
            throw reader.error("sobra texto depois do valor");
        }
        return value;
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    private Object value(int depth) {
        if (depth > MAX_DEPTH) {
            throw error("valor aninhado demais");
        }
        if (at >= text.length()) {
            throw error("o texto acabou antes do valor");
        }
        return switch (text.charAt(at)) {
            case '{' -> object(depth);
            case '[' -> array(depth);
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object(int depth) {
        Map<String, Object> fields = new LinkedHashMap<>();
        at++;
        skipSpaces();
        if (peek('}')) {
            at++;
            return Collections.unmodifiableMap(fields);
        }
        while (true) {
            skipSpaces();
            if (!peek('"')) {
                throw error("esperava o nome de um campo");
            }
            String name = string();
            skipSpaces();
            expect(':');
            skipSpaces();
            fields.put(name, value(depth + 1));
            skipSpaces();
            if (peek(',')) {
                at++;
                continue;
            }
            expect('}');
            return Collections.unmodifiableMap(fields);
        }
    }

    private List<Object> array(int depth) {
        List<Object> items = new ArrayList<>();
        at++;
        skipSpaces();
        if (peek(']')) {
            at++;
            return Collections.unmodifiableList(items);
        }
        while (true) {
            skipSpaces();
            items.add(value(depth + 1));
            skipSpaces();
            if (peek(',')) {
                at++;
                continue;
            }
            expect(']');
            return Collections.unmodifiableList(items);
        }
    }

    private String string() {
        at++;
        StringBuilder out = new StringBuilder();
        while (at < text.length()) {
            char c = text.charAt(at++);
            if (c == '"') {
                return out.toString();
            }
            if (c < 0x20) {
                throw error("caractere de controle dentro do texto");
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            if (at >= text.length()) {
                break;
            }
            char escaped = text.charAt(at++);
            switch (escaped) {
                case '"', '\\', '/' -> out.append(escaped);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> out.append(unicode());
                default -> throw error("escape inválido \\" + escaped);
            }
        }
        throw error("texto sem aspas de fechamento");
    }

    private char unicode() {
        if (at + 4 > text.length()) {
            throw error("escape \\u incompleto");
        }
        int code = 0;
        for (int i = 0; i < 4; i++) {
            int digit = Character.digit(text.charAt(at + i), 16);
            if (digit < 0) {
                throw error("escape \\u inválido");
            }
            code = code * 16 + digit;
        }
        at += 4;
        return (char) code;
    }

    private BigDecimal number() {
        int start = at;
        while (at < text.length() && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) {
            at++;
        }
        String candidate = text.substring(start, at);
        if (!NUMBER.matcher(candidate).matches()) {
            throw error("valor inesperado");
        }
        try {
            return new BigDecimal(candidate);
        } catch (NumberFormatException e) {
            throw error("número fora do alcance");
        }
    }

    private Object literal(String word, Object value) {
        if (!text.startsWith(word, at)) {
            throw error("valor inesperado");
        }
        at += word.length();
        return value;
    }

    private boolean peek(char c) {
        return at < text.length() && text.charAt(at) == c;
    }

    private void expect(char c) {
        if (!peek(c)) {
            throw error("esperava '" + c + "'");
        }
        at++;
    }

    private void skipSpaces() {
        while (at < text.length() && " \t\n\r".indexOf(text.charAt(at)) >= 0) {
            at++;
        }
    }

    private MalformedJsonException error(String problem) {
        return new MalformedJsonException("JSON inválido na posição " + at + ": " + problem + ".");
    }

    private static void append(StringBuilder out, Object value) {
        switch (value) {
            case null -> out.append("null");
            case String text -> quote(out, text);
            case Boolean flag -> out.append(flag);
            case BigDecimal number -> out.append(plain(number));
            case Number number -> out.append(plain(new BigDecimal(number.toString())));
            case List<?> items -> {
                out.append('[');
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) {
                        out.append(',');
                    }
                    append(out, items.get(i));
                }
                out.append(']');
            }
            case Map<?, ?> fields -> {
                out.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> field : fields.entrySet()) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    quote(out, String.valueOf(field.getKey()));
                    out.append(':');
                    append(out, field.getValue());
                }
                out.append('}');
            }
            default -> quote(out, value.toString());
        }
    }

    private static String plain(BigDecimal number) {
        BigDecimal stripped = number.stripTrailingZeros();
        if (stripped.signum() == 0) {
            return "0";
        }
        int scale = stripped.scale();
        if (scale <= 0 && scale >= -64) {
            return stripped.toBigInteger().toString();
        }
        if (scale > 0 && scale <= 64) {
            return stripped.toPlainString();
        }
        return stripped.toString();
    }

    private static void quote(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
