package br.com.devready.application.practice.dto;

import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.domain.career.Competency;
import java.util.List;

public record PlacedProblem(
        ProblemSummary problem, Competency competency, List<String> askedBy, boolean inTrack) {}
