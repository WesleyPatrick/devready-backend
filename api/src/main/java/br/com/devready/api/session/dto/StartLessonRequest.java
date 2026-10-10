package br.com.devready.api.session.dto;

import jakarta.validation.constraints.NotBlank;

public record StartLessonRequest(@NotBlank(message = "Escolha a competência.") String competency) {}
