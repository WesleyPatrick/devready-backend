package br.com.devready.domain.practice;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public enum QuestionStatus {
    SOLVED,
    ATTEMPTED,
    NOT_STARTED;

    public static QuestionStatus of(Collection<Answer> answersToOneQuestion) {
        if (answersToOneQuestion.isEmpty()) {
            return NOT_STARTED;
        }
        return answersToOneQuestion.stream().anyMatch(Answer::correct) ? SOLVED : ATTEMPTED;
    }

    public static Map<UUID, QuestionStatus> byQuestion(Collection<Answer> answers) {
        Map<UUID, List<Answer>> grouped =
                answers.stream().collect(Collectors.groupingBy(Answer::questionId));
        Map<UUID, QuestionStatus> statuses = new HashMap<>();
        grouped.forEach((questionId, list) -> statuses.put(questionId, of(list)));
        return statuses;
    }

    public static QuestionStatus ofAttempts(Collection<ChallengeAttempt> attemptsToOneChallenge) {
        List<ChallengeAttempt> counted =
                attemptsToOneChallenge.stream().filter(ChallengeAttempt::counts).toList();
        if (counted.isEmpty()) {
            return NOT_STARTED;
        }
        return counted.stream().anyMatch(ChallengeAttempt::passed) ? SOLVED : ATTEMPTED;
    }

    public static Map<UUID, QuestionStatus> byChallenge(Collection<ChallengeAttempt> attempts) {
        Map<UUID, List<ChallengeAttempt>> grouped =
                attempts.stream()
                        .filter(ChallengeAttempt::counts)
                        .collect(Collectors.groupingBy(ChallengeAttempt::challengeId));
        Map<UUID, QuestionStatus> statuses = new HashMap<>();
        grouped.forEach((challengeId, list) -> statuses.put(challengeId, ofAttempts(list)));
        return statuses;
    }

    public static Map<UUID, QuestionStatus> withSolved(
            Map<UUID, QuestionStatus> statuses, Collection<UUID> solvedElsewhere) {
        Map<UUID, QuestionStatus> merged = new HashMap<>(statuses);
        solvedElsewhere.forEach(id -> merged.put(id, SOLVED));
        return merged;
    }
}
