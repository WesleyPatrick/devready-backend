package br.com.devready.api.practice.dto;

import br.com.devready.application.practice.dto.ProblemPage;
import java.util.List;

public record QuestionPageResponse(
        List<ProblemItemResponse> items, int page, int totalPages, int totalItems) {
    public static QuestionPageResponse from(ProblemPage page) {
        return new QuestionPageResponse(
                page.items().stream().map(ProblemItemResponse::from).toList(),
                page.page(),
                page.totalPages(),
                page.totalItems());
    }
}
