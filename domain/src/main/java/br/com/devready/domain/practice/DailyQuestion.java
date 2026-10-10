package br.com.devready.domain.practice;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DailyQuestion {

    private DailyQuestion() {}

    public static Optional<UUID> pick(
            List<UUID> trackQuestions,
            Collection<Answer> answers,
            Collection<UUID> solvedElsewhere,
            Instant now) {
        if (trackQuestions.isEmpty()) {
            return Optional.empty();
        }
        Instant dayStart = PracticeCalendar.startOf(PracticeCalendar.dayOf(now));
        Set<UUID> solvedBefore =
                answers.stream()
                        .filter(
                                answer ->
                                        answer.correct() && answer.answeredAt().isBefore(dayStart))
                        .map(Answer::questionId)
                        .collect(Collectors.toCollection(HashSet::new));
        solvedBefore.addAll(solvedElsewhere);
        return trackQuestions.stream()
                .filter(id -> !solvedBefore.contains(id))
                .findFirst()
                .or(() -> Optional.of(trackQuestions.get(0)));
    }
}
