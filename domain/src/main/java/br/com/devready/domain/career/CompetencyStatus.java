package br.com.devready.domain.career;

import java.util.Collection;

public enum CompetencyStatus {
    DONE,
    IN_PROGRESS,
    NOT_STARTED;

    public static CompetencyStatus of(Collection<QuestionProgress> questions) {
        if (questions.isEmpty()) {
            return NOT_STARTED;
        }
        if (questions.stream().allMatch(progress -> progress == QuestionProgress.SOLVED)) {
            return DONE;
        }
        if (questions.stream().anyMatch(progress -> progress != QuestionProgress.NOT_STARTED)) {
            return IN_PROGRESS;
        }
        return NOT_STARTED;
    }
}
