package br.com.devready.api.practice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AnswerRequest(@NotNull(message = "Escolha uma alternativa.") UUID optionId) {}
