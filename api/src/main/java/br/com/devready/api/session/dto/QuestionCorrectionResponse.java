package br.com.devready.api.session.dto;

import br.com.devready.application.session.dto.QuestionCorrectionView;
import java.util.UUID;

public record QuestionCorrectionResponse(
        int position,
        String slug,
        String title,
        boolean correct,
        UUID chosenOptionId,
        UUID correctOptionId,
        String explanation) {

    public static QuestionCorrectionResponse from(QuestionCorrectionView v) {
        return new QuestionCorrectionResponse(
                v.position(),
                v.slug(),
                v.title(),
                v.correct(),
                v.chosenOptionId(),
                v.correctOptionId(),
                v.explanation());
    }
}
