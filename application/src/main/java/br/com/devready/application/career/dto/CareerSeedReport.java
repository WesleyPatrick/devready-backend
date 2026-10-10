package br.com.devready.application.career.dto;

public record CareerSeedReport(
        int insertedCompanies,
        int updatedCompanies,
        int insertedStacks,
        int updatedStacks,
        int insertedCompetencies,
        int updatedCompetencies,
        int removedCompetencies,
        int insertedTracks,
        int updatedTracks,
        int removedTracks) {
    public int total() {
        return insertedCompanies
                + updatedCompanies
                + insertedStacks
                + updatedStacks
                + insertedCompetencies
                + updatedCompetencies
                + removedCompetencies
                + insertedTracks
                + updatedTracks
                + removedTracks;
    }
}
