package br.com.devready.api.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeRequest(@NotBlank(message = "Informe o código.") String code) {}
