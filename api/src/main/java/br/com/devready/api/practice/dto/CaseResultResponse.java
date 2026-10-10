package br.com.devready.api.practice.dto;

import br.com.devready.application.practice.dto.CaseView;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaseResultResponse(
        int position,
        boolean visible,
        String status,
        @JsonRawValue String args,
        @JsonRawValue String expected,
        @JsonRawValue String actual,
        String error,
        Integer timeMs) {
    public static CaseResultResponse from(CaseView view) {
        return new CaseResultResponse(
                view.position(),
                view.visible(),
                view.status().name(),
                view.argsJson(),
                view.expectedJson(),
                view.actualJson(),
                view.error(),
                view.timeMs());
    }
}
