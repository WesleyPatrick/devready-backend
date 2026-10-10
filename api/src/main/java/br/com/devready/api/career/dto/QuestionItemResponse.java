package br.com.devready.api.career.dto;

import br.com.devready.application.career.dto.TrackQuestionView;

public record QuestionItemResponse(
        String slug, String title, String difficulty, String level, String status) {
    public static QuestionItemResponse from(TrackQuestionView question) {
        return new QuestionItemResponse(
                question.slug(),
                question.title(),
                question.difficulty().name(),
                question.level().name(),
                question.progress().name());
    }
}
