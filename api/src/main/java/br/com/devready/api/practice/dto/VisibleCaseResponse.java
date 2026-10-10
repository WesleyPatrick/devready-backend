package br.com.devready.api.practice.dto;

import br.com.devready.domain.catalog.TestCase;
import com.fasterxml.jackson.annotation.JsonRawValue;

public record VisibleCaseResponse(
        int position, @JsonRawValue String args, @JsonRawValue String expected) {
    public static VisibleCaseResponse from(TestCase testCase) {
        return new VisibleCaseResponse(
                testCase.position(), testCase.argsJson(), testCase.expectedJson());
    }
}
