public class Main {

    private static final String MARKER = "__DEVREADY_MARKER__";
    private static final String FUNCTION = "__DEVREADY_FUNCTION__";

    public static void main(String[] args) throws Exception {
        java.io.PrintStream out =
                new java.io.PrintStream(
                        new java.io.FileOutputStream(java.io.FileDescriptor.out), true, "US-ASCII");
        System.setOut(System.err);
        String input =
                new String(System.in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        java.util.List<?> cases = (java.util.List<?>) new JsonIn(input).read();
        for (Object item : cases) {
            java.util.Map<?, ?> testCase = (java.util.Map<?, ?>) item;
            Object output = null;
            String error = null;
            long started = System.nanoTime();
            try {
                output = call((java.util.List<?>) testCase.get("args"));
            } catch (java.lang.reflect.InvocationTargetException e) {
                error = describe(e.getCause());
            } catch (Throwable e) {
                error = describe(e);
            }
            long timeMs = (System.nanoTime() - started) / 1000000L;
            String outputJson = "null";
            if (error == null) {
                try {
                    outputJson = JsonOut.write(output);
                } catch (Throwable e) {
                    error = "Saída que não vira JSON: " + describe(e);
                }
            }
            out.println(
                    MARKER
                            + "{\"position\":"
                            + JsonOut.write(testCase.get("position"))
                            + ",\"output\":"
                            + outputJson
                            + ",\"error\":"
                            + JsonOut.write(error)
                            + ",\"timeMs\":"
                            + timeMs
                            + "}");
            out.flush();
        }
    }

    private static Object call(java.util.List<?> arguments) throws Exception {
        java.lang.reflect.Method method = find(arguments.size());
        java.lang.reflect.Type[] types = method.getGenericParameterTypes();
        Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            values[i] = convert(arguments.get(i), types[i]);
        }
        java.lang.reflect.Constructor<?> constructor = Solution.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object solution = constructor.newInstance();
        method.setAccessible(true);
        return method.invoke(solution, values);
    }

    private static java.lang.reflect.Method find(int arity) throws NoSuchMethodException {
        for (java.lang.reflect.Method method : Solution.class.getDeclaredMethods()) {
            if (method.getName().equals(FUNCTION)
                    && method.getParameterCount() == arity
                    && !method.isBridge()) {
                return method;
            }
        }
        throw new NoSuchMethodException(
                "Solution." + FUNCTION + " com " + arity + " parâmetro(s) não encontrado");
    }

    private static Object convert(Object value, java.lang.reflect.Type type) {
        if (type instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType generic = (java.lang.reflect.ParameterizedType) type;
            Class<?> raw = (Class<?>) generic.getRawType();
            if (raw.isAssignableFrom(java.util.ArrayList.class)) {
                if (value == null) {
                    return null;
                }
                java.lang.reflect.Type element = generic.getActualTypeArguments()[0];
                java.util.List<Object> list = new java.util.ArrayList<Object>();
                for (Object item : (java.util.List<?>) value) {
                    list.add(convert(item, element));
                }
                return list;
            }
            throw new IllegalArgumentException("Tipo de parâmetro não suportado: " + type.getTypeName());
        }
        if (type instanceof java.lang.reflect.WildcardType) {
            return convert(value, ((java.lang.reflect.WildcardType) type).getUpperBounds()[0]);
        }
        Class<?> target = (Class<?>) type;
        if (value == null) {
            if (target.isPrimitive()) {
                throw new IllegalArgumentException("Valor nulo para um parâmetro " + target.getName());
            }
            return null;
        }
        if (target.isArray()) {
            java.util.List<?> items = (java.util.List<?>) value;
            Class<?> component = target.getComponentType();
            Object array = java.lang.reflect.Array.newInstance(component, items.size());
            for (int i = 0; i < items.size(); i++) {
                java.lang.reflect.Array.set(array, i, convert(items.get(i), component));
            }
            return array;
        }
        if (target == int.class || target == Integer.class) {
            return Integer.valueOf(((java.math.BigDecimal) value).intValueExact());
        }
        if (target == long.class || target == Long.class) {
            return Long.valueOf(((java.math.BigDecimal) value).longValueExact());
        }
        if (target == double.class || target == Double.class) {
            return Double.valueOf(((java.math.BigDecimal) value).doubleValue());
        }
        if (target == float.class || target == Float.class) {
            return Float.valueOf(((java.math.BigDecimal) value).floatValue());
        }
        if (target == boolean.class || target == Boolean.class) {
            return (Boolean) value;
        }
        if (target == char.class || target == Character.class) {
            String text = (String) value;
            if (text.length() != 1) {
                throw new IllegalArgumentException("Esperava um caractere: " + text);
            }
            return Character.valueOf(text.charAt(0));
        }
        if (target == String.class || target == Object.class) {
            return value;
        }
        if (target.isAssignableFrom(java.util.ArrayList.class)) {
            return value;
        }
        throw new IllegalArgumentException("Tipo de parâmetro não suportado: " + target.getName());
    }

    private static String describe(Throwable error) {
        if (error == null) {
            return "Erro sem descrição";
        }
        String text = error.toString();
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }

    static final class JsonIn {
        private final String text;
        private int at;

        JsonIn(String text) {
            this.text = text;
        }

        Object read() {
            skip();
            Object value = value(0);
            skip();
            if (at != text.length()) {
                throw error();
            }
            return value;
        }

        private Object value(int depth) {
            if (depth > 512 || at >= text.length()) {
                throw error();
            }
            char c = text.charAt(at);
            if (c == '{') {
                return object(depth);
            }
            if (c == '[') {
                return array(depth);
            }
            if (c == '"') {
                return string();
            }
            if (text.startsWith("true", at)) {
                at += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", at)) {
                at += 5;
                return Boolean.FALSE;
            }
            if (text.startsWith("null", at)) {
                at += 4;
                return null;
            }
            return number();
        }

        private java.util.Map<String, Object> object(int depth) {
            java.util.Map<String, Object> fields = new java.util.LinkedHashMap<String, Object>();
            at++;
            skip();
            if (peek('}')) {
                at++;
                return fields;
            }
            while (true) {
                skip();
                String name = string();
                skip();
                expect(':');
                skip();
                fields.put(name, value(depth + 1));
                skip();
                if (peek(',')) {
                    at++;
                    continue;
                }
                expect('}');
                return fields;
            }
        }

        private java.util.List<Object> array(int depth) {
            java.util.List<Object> items = new java.util.ArrayList<Object>();
            at++;
            skip();
            if (peek(']')) {
                at++;
                return items;
            }
            while (true) {
                skip();
                items.add(value(depth + 1));
                skip();
                if (peek(',')) {
                    at++;
                    continue;
                }
                expect(']');
                return items;
            }
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (at < text.length()) {
                char c = text.charAt(at++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                char escaped = text.charAt(at++);
                if (escaped == 'n') {
                    out.append('\n');
                } else if (escaped == 't') {
                    out.append('\t');
                } else if (escaped == 'r') {
                    out.append('\r');
                } else if (escaped == 'b') {
                    out.append('\b');
                } else if (escaped == 'f') {
                    out.append('\f');
                } else if (escaped == 'u') {
                    out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                    at += 4;
                } else {
                    out.append(escaped);
                }
            }
            throw error();
        }

        private java.math.BigDecimal number() {
            int start = at;
            while (at < text.length() && "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) {
                at++;
            }
            if (start == at) {
                throw error();
            }
            return new java.math.BigDecimal(text.substring(start, at));
        }

        private boolean peek(char c) {
            return at < text.length() && text.charAt(at) == c;
        }

        private void expect(char c) {
            if (!peek(c)) {
                throw error();
            }
            at++;
        }

        private void skip() {
            while (at < text.length() && Character.isWhitespace(text.charAt(at))) {
                at++;
            }
        }

        private IllegalArgumentException error() {
            return new IllegalArgumentException("Entrada dos casos ilegível na posição " + at);
        }
    }

    static final class JsonOut {
        static String write(Object value) {
            StringBuilder out = new StringBuilder();
            append(out, value, 0);
            return out.toString();
        }

        private static void append(StringBuilder out, Object value, int depth) {
            if (depth > 512) {
                throw new IllegalStateException("Estrutura aninhada demais");
            }
            if (value == null) {
                out.append("null");
                return;
            }
            if (value instanceof String) {
                quote(out, (String) value);
                return;
            }
            if (value instanceof Character) {
                quote(out, value.toString());
                return;
            }
            if (value instanceof Boolean) {
                out.append(value.toString());
                return;
            }
            if (value instanceof Double || value instanceof Float) {
                double number = ((Number) value).doubleValue();
                if (Double.isNaN(number) || Double.isInfinite(number)) {
                    throw new IllegalStateException("Número não finito: " + number);
                } else {
                    out.append(Double.toString(number));
                }
                return;
            }
            if (value instanceof java.math.BigDecimal) {
                out.append(((java.math.BigDecimal) value).toPlainString());
                return;
            }
            if (value instanceof Number) {
                out.append(value.toString());
                return;
            }
            if (value.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(value);
                out.append('[');
                for (int i = 0; i < length; i++) {
                    if (i > 0) {
                        out.append(',');
                    }
                    append(out, java.lang.reflect.Array.get(value, i), depth + 1);
                }
                out.append(']');
                return;
            }
            if (value instanceof java.util.Map) {
                out.append('{');
                boolean first = true;
                for (java.util.Map.Entry<?, ?> entry : ((java.util.Map<?, ?>) value).entrySet()) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    quote(out, String.valueOf(entry.getKey()));
                    out.append(':');
                    append(out, entry.getValue(), depth + 1);
                }
                out.append('}');
                return;
            }
            if (value instanceof Iterable) {
                out.append('[');
                boolean first = true;
                for (Object item : (Iterable<?>) value) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    append(out, item, depth + 1);
                }
                out.append(']');
                return;
            }
            quote(out, String.valueOf(value));
        }

        private static void quote(StringBuilder out, String text) {
            out.append('"');
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '"') {
                    out.append("\\\"");
                } else if (c == '\\') {
                    out.append("\\\\");
                } else if (c == '\n') {
                    out.append("\\n");
                } else if (c == '\r') {
                    out.append("\\r");
                } else if (c == '\t') {
                    out.append("\\t");
                } else if (c < 0x20 || c > 0x7e) {
                    out.append(String.format("\\u%04x", (int) c));
                } else {
                    out.append(c);
                }
            }
            out.append('"');
        }
    }
}
