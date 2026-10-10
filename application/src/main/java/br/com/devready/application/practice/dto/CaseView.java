package br.com.devready.application.practice.dto;

import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.evaluation.CaseResult;
import br.com.devready.domain.evaluation.CaseStatus;

public record CaseView(
        int position,
        boolean visible,
        CaseStatus status,
        String argsJson,
        String expectedJson,
        String actualJson,
        String error,
        Integer timeMs) {

    public static CaseView of(CaseResult result, TestCase testCase) {
        if (!result.visible() || testCase == null || !testCase.visible()) {
            return new CaseView(
                    result.position(),
                    result.visible(),
                    result.status(),
                    null,
                    null,
                    null,
                    null,
                    result.timeMs());
        }
        return new CaseView(
                result.position(),
                true,
                result.status(),
                testCase.argsJson(),
                testCase.expectedJson(),
                result.actualJson(),
                result.errorMessage(),
                result.timeMs());
    }
}
