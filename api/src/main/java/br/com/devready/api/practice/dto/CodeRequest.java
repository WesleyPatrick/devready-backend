package br.com.devready.api.practice.dto;

import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CodeRequest(
        @NotNull(message = "Escolha a linguagem.") Language language,
        @NotBlank(message = "Escreva o código antes de rodar ou enviar.")
                @Size(max = CodeSubmission.MAX_SOURCE_CHARS, message = "O código passa de 64 KB.")
                String sourceCode) {}
