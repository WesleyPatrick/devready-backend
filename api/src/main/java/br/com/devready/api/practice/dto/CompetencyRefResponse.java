package br.com.devready.api.practice.dto;

import br.com.devready.domain.career.Competency;

public record CompetencyRefResponse(String slug, String name, String category) {
    public static CompetencyRefResponse from(Competency competency) {
        return competency == null
                ? null
                : new CompetencyRefResponse(
                        competency.slug(), competency.name(), competency.category().slug());
    }
}
