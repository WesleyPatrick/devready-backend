package br.com.devready.api.session.dto;

import br.com.devready.api.practice.dto.OptionResponse;
import br.com.devready.application.session.dto.SessionQuestionView;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record SessionQuestionResponse(
        int position,
        String slug,
        String title,
        String difficulty,
        String statement,
        List<OptionResponse> options,
        UUID chosenOptionId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean correct,
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID correctOptionId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String explanation) {

    public static SessionQuestionResponse from(SessionQuestionView view) {
        Question question = view.question();
        boolean revealed = view.revealed();
        return new SessionQuestionResponse(
                view.position(),
                question.slug(),
                question.title(),
                question.difficulty().name(),
                question.statement(),
                question.options().stream()
                        .sorted(Comparator.comparingInt(QuestionOption::position))
                        .map(option -> new OptionResponse(option.id(), option.text()))
                        .toList(),
                view.chosenOptionId(),
                revealed ? view.correct() : null,
                revealed ? question.correctOption().id() : null,
                revealed ? question.explanation() : null);
    }
}
