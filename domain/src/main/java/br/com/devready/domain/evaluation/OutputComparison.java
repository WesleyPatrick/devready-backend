package br.com.devready.domain.evaluation;

import br.com.devready.domain.catalog.ComparisonMode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OutputComparison {

    static final double TOLERANCE = 1e-6;

    private OutputComparison() {}

    public static boolean matches(Object expected, Object actual, ComparisonMode mode) {
        return switch (mode) {
            case EXACT -> equal(expected, actual, false);
            case FLOAT_TOLERANCE -> equal(expected, actual, true);
            case UNORDERED ->
                    expected instanceof List<?> wanted && actual instanceof List<?> got
                            ? sameItems(wanted, got)
                            : equal(expected, actual, false);
        };
    }

    private static boolean sameItems(List<?> wanted, List<?> got) {
        if (wanted.size() != got.size()) {
            return false;
        }
        List<Object> left = new ArrayList<>(got);
        for (Object item : wanted) {
            int match = -1;
            for (int i = 0; i < left.size(); i++) {
                if (equal(item, left.get(i), false)) {
                    match = i;
                    break;
                }
            }
            if (match < 0) {
                return false;
            }
            left.remove(match);
        }
        return true;
    }

    private static boolean equal(Object a, Object b, boolean tolerant) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            if (x.compareTo(y) == 0) {
                return true;
            }
            return tolerant && Math.abs(x.doubleValue() - y.doubleValue()) <= TOLERANCE;
        }
        if (a instanceof List<?> xs && b instanceof List<?> ys) {
            if (xs.size() != ys.size()) {
                return false;
            }
            for (int i = 0; i < xs.size(); i++) {
                if (!equal(xs.get(i), ys.get(i), tolerant)) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof Map<?, ?> xm && b instanceof Map<?, ?> ym) {
            if (!xm.keySet().equals(ym.keySet())) {
                return false;
            }
            for (Map.Entry<?, ?> field : xm.entrySet()) {
                if (!equal(field.getValue(), ym.get(field.getKey()), tolerant)) {
                    return false;
                }
            }
            return true;
        }
        return a.equals(b);
    }
}
