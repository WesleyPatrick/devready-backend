package br.com.devready.application.session.dto;

import java.util.UUID;

public record QuestionCorrectionView(
        int position,
        String slug,
        String title,
        boolean correct,
        UUID chosenOptionId,
        UUID correctOptionId,
        String explanation) {}
