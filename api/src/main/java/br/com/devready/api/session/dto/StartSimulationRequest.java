package br.com.devready.api.session.dto;

import br.com.devready.domain.catalog.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartSimulationRequest(
        @NotBlank(message = "Escolha a área.") String area,
        @NotNull(message = "Escolha o nível.") Level level) {}
