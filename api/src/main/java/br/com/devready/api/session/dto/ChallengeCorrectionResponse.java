package br.com.devready.api.session.dto;

import br.com.devready.application.session.dto.ChallengeCorrectionView;
import java.util.UUID;

public record ChallengeCorrectionResponse(
        int position,
        String slug,
        String title,
        int passedCount,
        int totalCount,
        UUID bestSubmissionId) {

    public static ChallengeCorrectionResponse from(ChallengeCorrectionView v) {
        return new ChallengeCorrectionResponse(
                v.position(),
                v.slug(),
                v.title(),
                v.passedCount(),
                v.totalCount(),
                v.bestSubmissionId());
    }
}
