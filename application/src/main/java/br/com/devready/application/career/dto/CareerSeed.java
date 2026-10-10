package br.com.devready.application.career.dto;

import java.util.List;

public record CareerSeed(
        List<CompanySeed> companies,
        List<StackSeed> stacks,
        List<CompetencySeed> competencies,
        List<MaterialSeed> materials,
        List<TrackSeed> tracks) {
    public CareerSeed {
        companies = companies == null ? List.of() : companies;
        stacks = stacks == null ? List.of() : stacks;
        competencies = competencies == null ? List.of() : competencies;
        materials = materials == null ? List.of() : materials;
        tracks = tracks == null ? List.of() : tracks;
    }
}
