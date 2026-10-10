package br.com.devready.api.career.dto;

import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.catalog.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveCareerProfileRequest(
        @NotNull(message = "Escolha pelo menos uma empresa.")
                @Size(
                        min = 1,
                        max = CareerProfile.MAX_COMPANIES,
                        message = "Escolha de 1 a 5 empresas.")
                List<
                                @NotBlank(message = "Empresa em branco na lista.")
                                @Size(max = 50, message = "Slug de empresa longo demais.") String>
                        companies,
        @NotBlank(message = "Escolha uma stack.")
                @Size(max = 50, message = "Slug de stack longo demais.")
                String stack,
        @NotNull(message = "Escolha a senioridade.") Level seniority) {}
