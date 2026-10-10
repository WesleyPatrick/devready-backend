package br.com.devready.application.catalog.dto;

public record AreaSeed(
        String slug,
        String name,
        int questionCount,
        int challengeCount,
        int durationMinutes,
        double objectiveWeight) {}
