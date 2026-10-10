package br.com.devready.domain.practice;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

public record FirstAttempts(int people, int correct) {

    public static final int MIN_PEOPLE = 5;

    public FirstAttempts {
        if (people < 0 || correct < 0 || correct > people) {
            throw new PracticeException(
                    "Contagem de primeiras respostas inválida: " + correct + " de " + people + ".");
        }
    }

    public OptionalDouble acceptance() {
        return people < MIN_PEOPLE
                ? OptionalDouble.empty()
                : OptionalDouble.of((double) correct / people);
    }

    public static Map<UUID, FirstAttempts> byQuestion(Collection<Answer> answers) {
        Map<UUID, Map<UUID, Answer>> firstByPerson = new HashMap<>();
        for (Answer answer : answers) {
            firstByPerson
                    .computeIfAbsent(answer.questionId(), id -> new HashMap<>())
                    .merge(
                            answer.userId(),
                            answer,
                            (kept, other) ->
                                    other.answeredAt().isBefore(kept.answeredAt()) ? other : kept);
        }
        Map<UUID, FirstAttempts> result = new HashMap<>();
        firstByPerson.forEach(
                (questionId, firsts) -> {
                    int right = (int) firsts.values().stream().filter(Answer::correct).count();
                    result.put(questionId, new FirstAttempts(firsts.size(), right));
                });
        return result;
    }

    public static Map<UUID, FirstAttempts> byChallenge(Collection<ChallengeAttempt> attempts) {
        Map<UUID, Map<UUID, ChallengeAttempt>> firstByPerson = new HashMap<>();
        for (ChallengeAttempt attempt : attempts) {
            if (!attempt.counts()) {
                continue;
            }
            firstByPerson
                    .computeIfAbsent(attempt.challengeId(), id -> new HashMap<>())
                    .merge(
                            attempt.userId(),
                            attempt,
                            (kept, other) ->
                                    other.submittedAt().isBefore(kept.submittedAt())
                                            ? other
                                            : kept);
        }
        Map<UUID, FirstAttempts> result = new HashMap<>();
        firstByPerson.forEach(
                (challengeId, firsts) -> {
                    int right =
                            (int) firsts.values().stream().filter(ChallengeAttempt::passed).count();
                    result.put(challengeId, new FirstAttempts(firsts.size(), right));
                });
        return result;
    }
}
