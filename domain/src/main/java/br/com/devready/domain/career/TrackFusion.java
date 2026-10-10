package br.com.devready.domain.career;

import br.com.devready.domain.catalog.Level;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TrackFusion {

    private TrackFusion() {}

    public static FusedTrack fuse(
            TrackGoal goal, List<Track> tracks, Map<String, Competency> competencies) {
        List<String> usedCompanies = new ArrayList<>();
        List<Track> used = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String company : goal.companySlugs()) {
            Optional<Track> chosen = choose(company, goal, tracks);
            if (chosen.isPresent()) {
                usedCompanies.add(company);
                used.add(chosen.get());
            } else {
                missing.add(company);
            }
        }
        if (used.isEmpty()) {
            return new FusedTrack(List.of(), 0, missing);
        }

        Map<String, Accumulator> bySlug = new LinkedHashMap<>();
        for (int i = 0; i < used.size(); i++) {
            String company = usedCompanies.get(i);
            for (TrackUnit unit : used.get(i).units()) {
                bySlug.computeIfAbsent(unit.competencySlug(), slug -> new Accumulator())
                        .add(company, unit);
            }
        }

        List<Ranked> ranked = new ArrayList<>();
        bySlug.forEach(
                (slug, accumulator) -> {
                    Competency competency = competencies.get(slug);
                    if (competency == null) {
                        throw new CareerException(
                                "Trilha usa a competência desconhecida " + slug + ".");
                    }
                    int positionSum = 0;
                    for (Track track : used) {
                        int position = track.positionOf(slug);
                        positionSum += position >= 0 ? position : track.units().size();
                    }
                    FusedUnit unit =
                            new FusedUnit(
                                    slug,
                                    accumulator.weight(used.size()),
                                    accumulator.obligatory
                                            ? Requirement.OBRIGATORIA
                                            : Requirement.DESEJAVEL,
                                    accumulator.companies,
                                    accumulator.jobs,
                                    (double) positionSum / used.size());
                    ranked.add(new Ranked(competency, positionSum, unit));
                });
        ranked.sort(
                Comparator.comparing((Ranked r) -> r.competency().category())
                        .thenComparingInt(Ranked::positionSum)
                        .thenComparing((Ranked r) -> r.unit().weight(), Comparator.reverseOrder())
                        .thenComparing(
                                (Ranked r) -> r.competency().name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing((Ranked r) -> r.competency().slug()));

        int jobsAnalyzed = used.stream().mapToInt(Track::jobs).sum();
        return new FusedTrack(ranked.stream().map(Ranked::unit).toList(), jobsAnalyzed, missing);
    }

    private static Optional<Track> choose(String company, TrackGoal goal, List<Track> tracks) {
        List<Track> candidates =
                tracks.stream()
                        .filter(
                                t ->
                                        t.companySlug().equals(company)
                                                && t.areaSlug().equals(goal.areaSlug()))
                        .toList();
        return find(candidates, goal.stackSlug(), goal.seniority())
                .or(() -> find(candidates, null, goal.seniority()))
                .or(() -> find(candidates, goal.stackSlug(), null))
                .or(() -> find(candidates, null, null));
    }

    private static Optional<Track> find(List<Track> candidates, String stackSlug, Level seniority) {
        return candidates.stream()
                .filter(t -> Objects.equals(t.stackSlug(), stackSlug) && t.seniority() == seniority)
                .findFirst();
    }

    private static final class Accumulator {
        private final List<String> companies = new ArrayList<>();
        private BigDecimal weightSum = BigDecimal.ZERO;
        private int jobs;
        private boolean obligatory;

        void add(String company, TrackUnit unit) {
            companies.add(company);
            weightSum = weightSum.add(BigDecimal.valueOf(unit.weight()));
            jobs += unit.jobs();
            obligatory |= unit.requirement() == Requirement.OBRIGATORIA;
        }

        double weight(int usedCompanies) {
            return weightSum
                    .divide(BigDecimal.valueOf(usedCompanies), 3, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }

    private record Ranked(Competency competency, int positionSum, FusedUnit unit) {}
}
