package br.com.devready.domain.career;

import java.util.List;

public record FusedTrack(List<FusedUnit> units, int jobsAnalyzed, List<String> missingCompanies) {
    public FusedTrack {
        units = List.copyOf(units);
        missingCompanies = List.copyOf(missingCompanies);
    }
}
