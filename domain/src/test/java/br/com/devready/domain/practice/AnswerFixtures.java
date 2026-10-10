package br.com.devready.domain.practice;

import br.com.devready.domain.evaluation.SubmissionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

final class AnswerFixtures {

    static final LocalDate DAY = LocalDate.ofEpochDay(20_000);

    static final UUID ANA = UUID.randomUUID();
    static final UUID BIA = UUID.randomUUID();

    private AnswerFixtures() {}

    static Instant at(int days, int hour, int minute) {
        return ZonedDateTime.of(
                        DAY.plusDays(days), LocalTime.of(hour, minute), PracticeCalendar.ZONE)
                .toInstant();
    }

    static Answer answer(UUID user, UUID question, boolean correct, int xp, Instant at) {
        return new Answer(UUID.randomUUID(), user, question, UUID.randomUUID(), correct, xp, at);
    }

    static ChallengeAttempt attempt(
            UUID user, UUID challenge, SubmissionStatus status, int xp, Instant at) {
        return new ChallengeAttempt(user, challenge, status, xp, at);
    }
}
