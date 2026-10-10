package br.com.devready.domain.career;

import br.com.devready.domain.catalog.Level;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record Track(
        UUID id,
        String companySlug,
        String areaSlug,
        String stackSlug,
        Level seniority,
        int jobs,
        List<TrackUnit> units) {
    public Track {
        if (id == null) {
            throw new CareerException("Trilha sem id.");
        }
        Names.slug(companySlug, "empresa da trilha");
        Names.slug(areaSlug, "área da trilha");
        if (stackSlug != null) {
            Names.slug(stackSlug, "stack da trilha");
        }
        String label = label(companySlug, areaSlug, stackSlug, seniority);
        if (jobs < 1) {
            throw new CareerException("Trilha " + label + " precisa de ao menos uma vaga.");
        }
        if (units == null || units.isEmpty()) {
            throw new CareerException("Trilha " + label + " sem competências.");
        }
        Set<String> seen = new HashSet<>();
        for (TrackUnit unit : units) {
            if (unit == null) {
                throw new CareerException("Unidade vazia na trilha " + label + ".");
            }
            if (!seen.add(unit.competencySlug())) {
                throw new CareerException(
                        "Competência "
                                + unit.competencySlug()
                                + " repetida na trilha "
                                + label
                                + ".");
            }
            if (unit.jobs() > jobs) {
                throw new CareerException(
                        "Competência "
                                + unit.competencySlug()
                                + " com mais vagas que a trilha "
                                + label
                                + ".");
            }
        }
        units = List.copyOf(units);
    }

    public String label() {
        return label(companySlug, areaSlug, stackSlug, seniority);
    }

    public static String label(
            String companySlug, String areaSlug, String stackSlug, Level seniority) {
        return companySlug
                + "/"
                + areaSlug
                + "/"
                + (seniority == null ? "todas" : seniority.name().toLowerCase(Locale.ROOT))
                + "/"
                + (stackSlug == null ? "todas" : stackSlug);
    }

    public int positionOf(String competencySlug) {
        for (int i = 0; i < units.size(); i++) {
            if (units.get(i).competencySlug().equals(competencySlug)) {
                return i;
            }
        }
        return -1;
    }
}
