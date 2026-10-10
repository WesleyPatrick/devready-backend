package br.com.devready.api.session.dto;

import br.com.devready.api.practice.dto.CompetencyRefResponse;
import br.com.devready.application.session.dto.CompetencyResultView;

public record CompetencyResultResponse(
        CompetencyRefResponse competency, int hits, int total, String verdict, String text) {

    public static CompetencyResultResponse from(CompetencyResultView view) {
        return new CompetencyResultResponse(
                CompetencyRefResponse.from(view.competency()),
                view.feedback().hits(),
                view.feedback().total(),
                view.feedback().verdict().name(),
                view.feedback().text());
    }
}
