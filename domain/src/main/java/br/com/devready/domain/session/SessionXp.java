package br.com.devready.domain.session;

import br.com.devready.domain.catalog.Level;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SessionXp {

    public static final int MIN = 10;

    private SessionXp() {}

    public static int of(double finalScore, Level level) {
        BigDecimal raw =
                BigDecimal.valueOf(finalScore)
                        .multiply(BigDecimal.valueOf(100))
                        .multiply(BigDecimal.valueOf(level.xpMultiplier()));
        return Math.max(MIN, raw.setScale(0, RoundingMode.HALF_UP).intValueExact());
    }
}
