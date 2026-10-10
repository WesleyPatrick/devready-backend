package br.com.devready.domain.catalog;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum Level {
    JUNIOR(1.0),
    PLENO(1.5),
    SENIOR(2.0);

    private final double xpMultiplier;

    Level(double xpMultiplier) {
        this.xpMultiplier = xpMultiplier;
    }

    public double xpMultiplier() {
        return xpMultiplier;
    }

    public static Set<Level> upTo(Level seniority) {
        if (seniority == null) {
            throw new CatalogException("Informe a senioridade para saber os níveis visíveis.");
        }
        return Collections.unmodifiableSet(EnumSet.range(JUNIOR, seniority));
    }
}
