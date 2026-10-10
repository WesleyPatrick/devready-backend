package br.com.devready.application.career.dto;

import br.com.devready.domain.career.Competency;
import br.com.devready.domain.career.CompetencyStatus;
import br.com.devready.domain.career.Requirement;
import java.util.List;

public record TrackCompetencyView(
        Competency competency,
        Requirement requirement,
        List<String> askedBy,
        int jobs,
        double weight,
        CompetencyStatus status) {}
