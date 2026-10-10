package br.com.devready.domain.career;

import java.util.List;

public record FusedUnit(
        String competencySlug,
        double weight,
        Requirement requirement,
        List<String> askedBy,
        int jobs,
        double meanPosition) {
    public FusedUnit {
        askedBy = List.copyOf(askedBy);
    }
}
