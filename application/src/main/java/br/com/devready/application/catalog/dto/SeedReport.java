package br.com.devready.application.catalog.dto;

public record SeedReport(
        int areas,
        int tags,
        int questions,
        int challenges,
        int updatedQuestions,
        int updatedChallenges) {
    public int total() {
        return areas + tags + questions + challenges + updatedQuestions + updatedChallenges;
    }
}
