package br.com.devready.application.practice.exception;

import java.util.UUID;

public class QuestionInExamException extends ProblemInExamException {
    public QuestionInExamException(UUID sessionId) {
        super("Esta questão está na sua simulação em andamento.", sessionId);
    }
}
