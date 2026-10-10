package br.com.devready.domain.evaluation;

public class MalformedJsonException extends EvaluationException {
    public MalformedJsonException(String message) {
        super(message);
    }
}
