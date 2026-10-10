package br.com.devready.api.practice.dto;

import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.application.practice.dto.ProblemView;
import java.util.List;

public record ProblemItemResponse(
        String slug,
        String type,
        String title,
        CompetencyRefResponse competency,
        String difficulty,
        String level,
        String status,
        Double acceptance,
        List<String> askedBy,
        boolean inExam) {

    public static ProblemItemResponse from(ProblemView view) {
        ProblemSummary problem = view.problem();
        return new ProblemItemResponse(
                problem.slug(),
                problem.type().name(),
                problem.title(),
                CompetencyRefResponse.from(view.competency()),
                problem.difficulty().name(),
                problem.level().name(),
                view.status().name(),
                view.acceptance(),
                view.askedBy(),
                view.inExam());
    }
}
