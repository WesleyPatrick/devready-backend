package br.com.devready.api.practice.dto;

import br.com.devready.application.practice.dto.QuestionView;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import java.util.Comparator;
import java.util.List;

public record QuestionResponse(
        String slug,
        String title,
        CompetencyRefResponse competency,
        String difficulty,
        String level,
        String status,
        int estimatedMinutes,
        String statement,
        List<OptionResponse> options,
        int xp) {

    public static QuestionResponse from(QuestionView view) {
        Question question = view.question();
        return new QuestionResponse(
                question.slug(),
                question.title(),
                CompetencyRefResponse.from(view.competency()),
                question.difficulty().name(),
                question.level().name(),
                view.status().name(),
                view.estimatedMinutes(),
                question.statement(),
                question.options().stream()
                        .sorted(Comparator.comparingInt(QuestionOption::position))
                        .map(option -> new OptionResponse(option.id(), option.text()))
                        .toList(),
                view.xp());
    }
}
