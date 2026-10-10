package br.com.devready.domain.session;

public record CompetencyFeedback(
        String competencySlug,
        String competencyName,
        int hits,
        int total,
        FeedbackVerdict verdict,
        String text) {}
