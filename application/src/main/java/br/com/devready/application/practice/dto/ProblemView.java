package br.com.devready.application.practice.dto;

import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.practice.QuestionStatus;
import java.util.List;

public record ProblemView(
        ProblemSummary problem,
        Competency competency,
        List<String> askedBy,
        QuestionStatus status,
        Double acceptance,
        boolean inExam) {}
