package br.com.devready.application.evaluation.dto;

import br.com.devready.domain.catalog.Language;

public record ExecutionRequest(Language language, String program, String stdin, int timeLimitMs) {
    public ExecutionRequest {
        if (language == null || program == null || stdin == null || timeLimitMs <= 0) {
            throw new IllegalArgumentException("Pedido de execução incompleto.");
        }
    }
}
