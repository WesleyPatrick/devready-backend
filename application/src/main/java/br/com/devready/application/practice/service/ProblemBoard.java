package br.com.devready.application.practice.service;

import br.com.devready.application.career.dto.LearningTrackView;
import br.com.devready.application.career.dto.TrackCompetencyView;
import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.PracticeCatalog;
import br.com.devready.application.practice.dto.PlacedProblem;
import br.com.devready.application.practice.dto.Placement;
import br.com.devready.application.shared.TitleOrder;
import br.com.devready.domain.career.Competency;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class ProblemBoard {

    private static final Comparator<Competency> BASE_ORDER =
            Comparator.comparing(Competency::category)
                    .thenComparing(Competency::name, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Competency::slug);
    private static final Comparator<ProblemSummary> WITHIN_COMPETENCY =
            Comparator.comparing(ProblemSummary::difficulty)
                    .thenComparing(ProblemSummary::title, TitleOrder.BY_TITLE)
                    .thenComparing(ProblemSummary::slug);

    private static final int OUTSIDE = Integer.MAX_VALUE;

    private record Ranked(int position, PlacedProblem placed) {}

    private final TrackAssembler assembler;
    private final AreaRepository areas;
    private final PracticeCatalog catalog;
    private final CompetencyRepository competencies;

    public ProblemBoard(
            TrackAssembler assembler,
            AreaRepository areas,
            PracticeCatalog catalog,
            CompetencyRepository competencies) {
        this.assembler = assembler;
        this.areas = areas;
        this.catalog = catalog;
        this.competencies = competencies;
    }

    public List<PlacedProblem> problemsOf(UUID userId) {
        LearningTrackView track = assembler.assemble(userId);
        List<TrackCompetencyView> inTrack = track.competencies();
        List<Competency> base = baseOrder();
        List<ProblemSummary> problems =
                areas.findBySlug(track.stack().areaSlug())
                        .map(area -> catalog.activeProblems(area.id(), track.visibleLevels()))
                        .orElse(List.of());
        return problems.stream()
                .map(
                        problem -> {
                            int position = positionIn(inTrack, problem.tagSlugs());
                            return new Ranked(position, place(problem, inTrack, position, base));
                        })
                .sorted(
                        Comparator.comparingInt(Ranked::position)
                                .thenComparing(
                                        ranked -> ranked.placed().problem(), WITHIN_COMPETENCY))
                .map(Ranked::placed)
                .toList();
    }

    public Placement placementOf(UUID userId, Set<String> tagSlugs) {
        return placer(userId).apply(tagSlugs);
    }

    public Function<Set<String>, Placement> placer(UUID userId) {
        List<TrackCompetencyView> inTrack =
                assembler.find(userId).map(LearningTrackView::competencies).orElse(List.of());
        List<List<Competency>> base = new ArrayList<>(1);
        return tagSlugs -> {
            int position = positionIn(inTrack, tagSlugs);
            if (position == OUTSIDE) {
                if (base.isEmpty()) {
                    base.add(baseOrder());
                }
                return new Placement(firstOf(base.get(0), tagSlugs), List.of());
            }
            TrackCompetencyView item = inTrack.get(position);
            return new Placement(item.competency(), item.askedBy());
        };
    }

    public Competency competencyOf(UUID userId, Set<String> tagSlugs) {
        return placementOf(userId, tagSlugs).competency();
    }

    private static PlacedProblem place(
            ProblemSummary problem,
            List<TrackCompetencyView> inTrack,
            int position,
            List<Competency> base) {
        if (position == OUTSIDE) {
            return new PlacedProblem(problem, firstOf(base, problem.tagSlugs()), List.of(), false);
        }
        TrackCompetencyView item = inTrack.get(position);
        return new PlacedProblem(problem, item.competency(), item.askedBy(), true);
    }

    private static int positionIn(List<TrackCompetencyView> inTrack, Set<String> tags) {
        for (int i = 0; i < inTrack.size(); i++) {
            if (shares(inTrack.get(i).competency().tagSlugs(), tags)) {
                return i;
            }
        }
        return OUTSIDE;
    }

    private List<Competency> baseOrder() {
        return competencies.findAll().stream().sorted(BASE_ORDER).toList();
    }

    private static Competency firstOf(List<Competency> base, Set<String> tags) {
        return base.stream()
                .filter(competency -> shares(competency.tagSlugs(), tags))
                .findFirst()
                .orElse(null);
    }

    private static boolean shares(Set<String> some, Set<String> others) {
        return some.stream().anyMatch(others::contains);
    }
}
