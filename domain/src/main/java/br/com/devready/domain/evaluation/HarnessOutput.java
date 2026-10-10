package br.com.devready.domain.evaluation;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class HarnessOutput {

    private HarnessOutput() {}

    public static Map<Integer, HarnessLine> parse(String stdout, String marker) {
        if (marker == null || marker.isBlank()) {
            throw new EvaluationException("Marcador do harness vazio.");
        }
        Map<Integer, HarnessLine> lines = new HashMap<>();
        if (stdout == null) {
            return lines;
        }
        for (String raw : stdout.split("\\R")) {
            int at = raw.indexOf(marker);
            if (at >= 0) {
                read(raw.substring(at + marker.length()))
                        .ifPresent(line -> lines.putIfAbsent(line.position(), line));
            }
        }
        return lines;
    }

    private static Optional<HarnessLine> read(String payload) {
        Object parsed;
        try {
            parsed = Json.parse(payload);
        } catch (MalformedJsonException e) {
            return Optional.empty();
        }
        if (!(parsed instanceof Map<?, ?> fields)
                || !(fields.get("position") instanceof BigDecimal position)) {
            return Optional.empty();
        }
        int index;
        try {
            index = position.intValueExact();
        } catch (ArithmeticException e) {
            return Optional.empty();
        }
        if (index < 0) {
            return Optional.empty();
        }
        String error = fields.get("error") instanceof String text && !text.isBlank() ? text : null;
        Integer timeMs =
                fields.get("timeMs") instanceof BigDecimal time
                        ? Math.max(0, time.intValue())
                        : null;
        return Optional.of(new HarnessLine(index, fields.get("output"), error, timeMs));
    }
}
