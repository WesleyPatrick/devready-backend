package br.com.devready.api.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.") String email,
        @NotBlank(message = "Informe a senha.")
                @Size(min = 8, max = 72, message = "A senha precisa ter de 8 a 72 caracteres.")
                String password,
        @NotBlank(message = "Informe seu nome.") @Size(max = 120, message = "Nome muito longo.")
                String displayName) {}
