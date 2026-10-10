package br.com.devready.application.career.usecase;

import br.com.devready.application.career.dto.LearningTrackView;
import br.com.devready.application.career.dto.TrackChallengeView;
import br.com.devready.application.career.dto.TrackCompetencyDetail;
import br.com.devready.application.career.dto.TrackCompetencyView;
import br.com.devready.application.career.dto.TrackQuestionView;
import br.com.devready.application.career.exception.CompetencyNotInTrackException;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.catalog.dto.ChallengeSummary;
import br.com.devready.application.catalog.dto.QuestionSummary;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.PracticeCatalog;
import br.com.devready.application.shared.TitleOrder;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Level;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class GetTrackCompetencyUseCase {

    private static final Comparator<QuestionSummary> QUESTION_ORDER =
            Comparator.comparing(QuestionSummary::difficulty)
                    .thenComparing(QuestionSummary::title, TitleOrder.BY_TITLE)
                    .thenComparing(QuestionSummary::slug);
    private static final Comparator<ChallengeSummary> CHALLENGE_ORDER =
            Comparator.comparing(ChallengeSummary::difficulty)
                    .thenComparing(ChallengeSummary::title, TitleOrder.BY_TITLE)
                    .thenComparing(ChallengeSummary::slug);

    private final TrackAssembler assembler;
    private final AreaRepository areas;
    private final PracticeCatalog catalog;

    public GetTrackCompetencyUseCase(
            TrackAssembler assembler, AreaRepository areas, PracticeCatalog catalog) {
        this.assembler = assembler;
        this.areas = areas;
        this.catalog = catalog;
    }

    public TrackCompetencyDetail execute(UUID userId, String competencySlug) {
        LearningTrackView track = assembler.assemble(userId);
        TrackCompetencyView item =
                track.competencies().stream()
                        .filter(c -> c.competency().slug().equals(competencySlug))
                        .findFirst()
                        .orElseThrow(() -> new CompetencyNotInTrackException(competencySlug));
        Set<String> tags = item.competency().tagSlugs();
        Optional<Area> area = areas.findBySlug(track.stack().areaSlug());
        if (tags.isEmpty() || area.isEmpty()) {
            return new TrackCompetencyDetail(item, track.jobsAnalyzed(), List.of(), List.of());
        }
        UUID areaId = area.get().id();
        Set<Level> levels = track.visibleLevels();
        List<TrackQuestionView> questions =
                catalog.activeQuestions(areaId, tags, levels).stream()
                        .sorted(QUESTION_ORDER)
                        .map(
                                q ->
                                        new TrackQuestionView(
                                                q.slug(),
                                                q.title(),
                                                q.difficulty(),
                                                q.level(),
                                                track.progressOf(q.slug())))
                        .toList();
        List<TrackChallengeView> challenges =
                catalog.activeChallenges(areaId, tags, levels).stream()
                        .sorted(CHALLENGE_ORDER)
                        .map(
                                c ->
                                        new TrackChallengeView(
                                                c.slug(),
                                                c.title(),
                                                c.difficulty(),
                                                c.level(),
                                                c.languages().stream().sorted().toList(),
                                                track.progressOf(c.slug())))
                        .toList();
        return new TrackCompetencyDetail(item, track.jobsAnalyzed(), questions, challenges);
    }
}
