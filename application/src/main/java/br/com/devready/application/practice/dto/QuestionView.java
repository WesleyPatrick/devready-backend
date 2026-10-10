package br.com.devready.application.practice.dto;

import br.com.devready.domain.career.Competency;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.practice.EstimatedMinutes;
import br.com.devready.domain.practice.QuestionStatus;
import br.com.devready.domain.practice.Xp;

public record QuestionView(
        Question question,
        Competency competency,
        QuestionStatus status,
        int estimatedMinutes,
        int xp) {

    public static QuestionView of(Question question, Competency competency, QuestionStatus status) {
        return new QuestionView(
                question,
                competency,
                status,
                EstimatedMinutes.of(question.difficulty()),
                Xp.firstSolve(question.difficulty()));
    }
}
