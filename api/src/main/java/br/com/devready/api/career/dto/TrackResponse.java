package br.com.devready.api.career.dto;

import br.com.devready.application.career.dto.LearningTrackView;
import java.util.List;

public record TrackResponse(TrackPlanResponse plan, List<TrackCompetencyResponse> competencies) {
    public static TrackResponse from(LearningTrackView view) {
        return new TrackResponse(
                TrackPlanResponse.from(view),
                view.competencies().stream()
                        .map(c -> TrackCompetencyResponse.from(c, view.jobsAnalyzed()))
                        .toList());
    }
}
