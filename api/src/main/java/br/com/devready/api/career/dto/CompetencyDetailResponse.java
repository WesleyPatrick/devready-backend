package br.com.devready.api.career.dto;

import br.com.devready.application.career.dto.TrackCompetencyDetail;
import br.com.devready.domain.career.Competency;
import java.util.List;

public record CompetencyDetailResponse(
        String slug,
        String name,
        String category,
        String difficulty,
        String status,
        String requirement,
        List<String> askedBy,
        FrequencyResponse frequency,
        String description,
        String lockedReason,
        List<MaterialResponse> materials,
        List<QuestionItemResponse> questions,
        List<ChallengeItemResponse> challenges) {

    public static CompetencyDetailResponse from(TrackCompetencyDetail detail) {
        TrackCompetencyResponse item =
                TrackCompetencyResponse.from(detail.competency(), detail.jobsAnalyzed());
        Competency competency = detail.competency().competency();
        return new CompetencyDetailResponse(
                item.slug(),
                item.name(),
                item.category(),
                item.difficulty(),
                item.status(),
                item.requirement(),
                item.askedBy(),
                item.frequency(),
                competency.summary(),
                null,
                competency.materials().stream().map(MaterialResponse::from).toList(),
                detail.questions().stream().map(QuestionItemResponse::from).toList(),
                detail.challenges().stream().map(ChallengeItemResponse::from).toList());
    }
}
