package br.com.devready.application.career.service;

import br.com.devready.application.career.dto.LearningTrackView;
import br.com.devready.application.career.dto.TrackCompetencyView;
import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.career.exception.UnknownStackException;
import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.career.port.CompanyRepository;
import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.application.career.port.QuestionProgressReader;
import br.com.devready.application.career.port.StackRepository;
import br.com.devready.application.career.port.TrackRepository;
import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.PracticeCatalog;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.career.CompetencyStatus;
import br.com.devready.domain.career.FusedTrack;
import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.career.Stack;
import br.com.devready.domain.career.TrackFusion;
import br.com.devready.domain.career.TrackGoal;
import br.com.devready.domain.catalog.Level;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TrackAssembler {

    private final CareerProfileRepository profiles;
    private final CompanyRepository companies;
    private final StackRepository stacks;
    private final CompetencyRepository competencies;
    private final TrackRepository tracks;
    private final AreaRepository areas;
    private final PracticeCatalog catalog;
    private final QuestionProgressReader progress;

    public TrackAssembler(
            CareerProfileRepository profiles,
            CompanyRepository companies,
            StackRepository stacks,
            CompetencyRepository competencies,
            TrackRepository tracks,
            AreaRepository areas,
            PracticeCatalog catalog,
            QuestionProgressReader progress) {
        this.profiles = profiles;
        this.companies = companies;
        this.stacks = stacks;
        this.competencies = competencies;
        this.tracks = tracks;
        this.areas = areas;
        this.catalog = catalog;
        this.progress = progress;
    }

    public LearningTrackView assemble(UUID userId) {
        return find(userId).orElseThrow(CareerProfileNotFoundException::new);
    }

    public Optional<LearningTrackView> find(UUID userId) {
        return profiles.findByUserId(userId).map(profile -> build(userId, profile));
    }

    private LearningTrackView build(UUID userId, CareerProfile profile) {
        Stack stack =
                stacks.findBySlug(profile.stackSlug())
                        .orElseThrow(() -> new UnknownStackException(profile.stackSlug()));
        Map<String, Company> chosen = new LinkedHashMap<>();
        for (String slug : profile.companySlugs()) {
            companies.findBySlug(slug).ifPresent(company -> chosen.put(slug, company));
        }
        Map<String, Competency> bySlug = new HashMap<>();
        competencies.findAll().forEach(competency -> bySlug.put(competency.slug(), competency));
        FusedTrack fused =
                TrackFusion.fuse(
                        new TrackGoal(
                                List.copyOf(chosen.keySet()),
                                stack.areaSlug(),
                                stack.slug(),
                                profile.seniority()),
                        tracks.findByCompanies(chosen.keySet()),
                        bySlug);
        Map<String, QuestionProgress> answered = progress.answeredBy(userId);
        List<ProblemSummary> areaProblems =
                problemsOf(stack.areaSlug(), Level.upTo(profile.seniority()));
        List<TrackCompetencyView> items =
                fused.units().stream()
                        .map(
                                unit -> {
                                    Competency competency = bySlug.get(unit.competencySlug());
                                    return new TrackCompetencyView(
                                            competency,
                                            unit.requirement(),
                                            unit.askedBy(),
                                            unit.jobs(),
                                            unit.weight(),
                                            statusOf(competency, areaProblems, answered));
                                })
                        .toList();
        List<Company> missing = fused.missingCompanies().stream().map(chosen::get).toList();
        return new LearningTrackView(
                stack, items, fused.jobsAnalyzed(), missing, answered, profile.seniority());
    }

    private List<ProblemSummary> problemsOf(String areaSlug, Set<Level> levels) {
        return areas.findBySlug(areaSlug)
                .map(area -> catalog.activeProblems(area.id(), levels))
                .orElse(List.of());
    }

    private static CompetencyStatus statusOf(
            Competency competency,
            List<ProblemSummary> problems,
            Map<String, QuestionProgress> answered) {
        return CompetencyStatus.of(
                problems.stream()
                        .filter(
                                problem ->
                                        problem.tagSlugs().stream()
                                                .anyMatch(competency.tagSlugs()::contains))
                        .map(
                                problem ->
                                        answered.getOrDefault(
                                                problem.slug(), QuestionProgress.NOT_STARTED))
                        .toList());
    }
}
