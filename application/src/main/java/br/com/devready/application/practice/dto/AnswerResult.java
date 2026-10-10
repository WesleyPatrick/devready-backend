package br.com.devready.application.practice.dto;

import java.util.UUID;

public record AnswerResult(
        boolean correct, UUID correctOptionId, String explanation, int xpGained) {}
