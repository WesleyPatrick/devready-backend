package br.com.devready.application.career.dto;

import java.util.List;

public record TrackCompetencyDetail(
        TrackCompetencyView competency,
        int jobsAnalyzed,
        List<TrackQuestionView> questions,
        List<TrackChallengeView> challenges) {}
