package br.com.devready.api.career.dto;

import br.com.devready.application.career.dto.TrackCompetencyView;
import br.com.devready.domain.career.Competency;
import java.util.List;

public record TrackCompetencyResponse(
        String slug,
        String name,
        String category,
        String difficulty,
        String status,
        String requirement,
        List<String> askedBy,
        FrequencyResponse frequency) {

    public static TrackCompetencyResponse from(TrackCompetencyView view, int jobsAnalyzed) {
        Competency competency = view.competency();
        return new TrackCompetencyResponse(
                competency.slug(),
                competency.name(),
                competency.category().slug(),
                competency.difficulty().name(),
                view.status().name(),
                view.requirement().slug(),
                view.askedBy(),
                new FrequencyResponse(view.jobs(), jobsAnalyzed));
    }
}
