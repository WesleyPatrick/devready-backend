package br.com.devready.domain.session;

public record FeedbackItem(String competencySlug, String competencyName, boolean hit) {
    public FeedbackItem {
        if (competencySlug != null && (competencyName == null || competencyName.isBlank())) {
            throw new SessionException("Item de feedback com competência sem nome.");
        }
    }
}
