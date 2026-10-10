package br.com.devready.domain.evaluation;

public record CaseResult(
        int position,
        boolean visible,
        CaseStatus status,
        String actualJson,
        String errorMessage,
        Integer timeMs) {
    public CaseResult {
        if (position < 0 || status == null) {
            throw new EvaluationException("Resultado de caso sem posição ou situação.");
        }
        if (timeMs != null && timeMs < 0) {
            throw new EvaluationException("Tempo negativo num caso.");
        }
    }
}
