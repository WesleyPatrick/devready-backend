package br.com.devready.application.career.dto;

import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.UUID;

public record SaveCareerProfileCommand(
        UUID userId, List<String> companies, String stack, Level seniority) {}
