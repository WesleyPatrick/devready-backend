package br.com.devready.application.catalog.dto;

import java.util.List;

public record CatalogSeed(
        List<AreaSeed> areas,
        List<TagSeed> tags,
        List<QuestionSeed> questions,
        List<ChallengeSeed> challenges) {}
