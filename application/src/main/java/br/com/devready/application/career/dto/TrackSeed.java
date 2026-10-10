package br.com.devready.application.career.dto;

import java.util.List;

public record TrackSeed(
        String company,
        String area,
        String stack,
        String seniority,
        Integer jobs,
        List<TrackUnitSeed> units) {
    public TrackSeed {
        units = units == null ? List.of() : units;
    }
}
