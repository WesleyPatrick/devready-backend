package br.com.devready.application.practice.exception;

import java.util.UUID;

public class ChallengeInExamException extends ProblemInExamException {
    public ChallengeInExamException(UUID sessionId) {
        super("Este desafio está na sua simulação em andamento.", sessionId);
    }
}
