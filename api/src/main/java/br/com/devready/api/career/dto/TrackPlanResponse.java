package br.com.devready.api.career.dto;

import br.com.devready.application.career.dto.LearningTrackView;
import br.com.devready.application.career.dto.TrackCompetencyView;
import br.com.devready.domain.career.CompetencyStatus;
import br.com.devready.domain.catalog.Difficulty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record TrackPlanResponse(
        int totalCompetencies,
        int doneCompetencies,
        Map<String, DifficultyCountResponse> byDifficulty,
        int jobsAnalyzed,
        List<CompanyRefResponse> missingCompanies) {

    public static TrackPlanResponse from(LearningTrackView view) {
        Map<String, DifficultyCountResponse> byDifficulty = new LinkedHashMap<>();
        for (Difficulty difficulty : Difficulty.values()) {
            List<TrackCompetencyView> ofDifficulty =
                    view.competencies().stream()
                            .filter(c -> c.competency().difficulty() == difficulty)
                            .toList();
            byDifficulty.put(
                    difficulty.name(),
                    new DifficultyCountResponse(done(ofDifficulty), ofDifficulty.size()));
        }
        return new TrackPlanResponse(
                view.competencies().size(),
                done(view.competencies()),
                byDifficulty,
                view.jobsAnalyzed(),
                view.missingCompanies().stream().map(CompanyRefResponse::from).toList());
    }

    private static int done(List<TrackCompetencyView> competencies) {
        return (int) competencies.stream().filter(c -> c.status() == CompetencyStatus.DONE).count();
    }
}
