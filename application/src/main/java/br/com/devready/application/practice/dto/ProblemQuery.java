package br.com.devready.application.practice.dto;

import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.ProblemType;
import br.com.devready.domain.practice.QuestionStatus;
import java.util.UUID;

public record ProblemQuery(
        UUID userId,
        int page,
        int size,
        ProblemType type,
        QuestionStatus status,
        Difficulty difficulty,
        String competency,
        String company,
        Level level) {
    public ProblemQuery {
        competency = competency == null || competency.isBlank() ? null : competency;
        company = company == null || company.isBlank() ? null : company;
    }
}
