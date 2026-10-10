package br.com.devready.api.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Informe seu nome.") @Size(max = 120, message = "Nome muito longo.")
                String displayName,
        @Size(max = 2048, message = "URL muito longa.")
                @Pattern(
                        regexp = "^$|^https://\\S+$",
                        message = "Use um endereço que comece com https://.")
                String avatarUrl,
        @Size(max = 72, message = "A senha pode ter no máximo 72 caracteres.") String newPassword,
        String currentPassword) {}
