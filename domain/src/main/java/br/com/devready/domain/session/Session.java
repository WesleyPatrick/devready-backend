package br.com.devready.domain.session;

import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.evaluation.CodeSubmission;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

public record Session(
        UUID id,
        UUID userId,
        SessionFormat format,
        UUID areaId,
        Level level,
        String competencySlug,
        SessionStatus status,
        Double objectiveWeight,
        Instant startedAt,
        Instant deadlineAt,
        Instant finishedAt,
        Double objectiveScore,
        Double challengeScore,
        Double finalScore,
        int xpAwarded,
        String failureReason,
        List<SessionQuestion> questions,
        List<SessionChallenge> challenges) {

    public static final Duration GRACE = Duration.ofSeconds(30);

    public Session {
        if (id == null
                || userId == null
                || format == null
                || areaId == null
                || level == null
                || status == null
                || startedAt == null) {
            throw new SessionException(
                    "Sessão sem id, pessoa, formato, área, nível, situação ou início.");
        }
        questions = questions == null ? List.of() : List.copyOf(questions);
        challenges = challenges == null ? List.of() : List.copyOf(challenges);
        if (format == SessionFormat.LESSON) {
            if (competencySlug == null || competencySlug.isBlank()) {
                throw new SessionException("Lição sem competência.");
            }
            if (deadlineAt != null || objectiveWeight != null || !challenges.isEmpty()) {
                throw new SessionException("Lição não tem prazo, peso nem desafio.");
            }
            if (xpAwarded != 0) {
                throw new SessionException("Lição não tem bônus de XP.");
            }
        } else {
            if (competencySlug != null) {
                throw new SessionException("Simulação não tem competência.");
            }
            if (deadlineAt == null || !deadlineAt.isAfter(startedAt)) {
                throw new SessionException("Simulação precisa de prazo depois do início.");
            }
            if (objectiveWeight == null || objectiveWeight < 0 || objectiveWeight > 1) {
                throw new SessionException("Peso das objetivas fora de 0 a 1.");
            }
        }
        if (questions.isEmpty() && challenges.isEmpty()) {
            throw new SessionException("Sessão sem itens.");
        }
        sequential(questions.stream().map(SessionQuestion::position).toList(), "questões");
        sequential(challenges.stream().map(SessionChallenge::position).toList(), "desafios");
        if (status.closed() != (finishedAt != null)) {
            throw new SessionException("Sessão terminada precisa da hora do fim, e só ela.");
        }
        if ((status == SessionStatus.FAILED) != (failureReason != null)) {
            throw new SessionException("Só a sessão com falha tem motivo.");
        }
        if ((status == SessionStatus.COMPLETED) != (objectiveScore != null)) {
            throw new SessionException("Só a sessão corrigida tem nota.");
        }
        if (xpAwarded < 0) {
            throw new SessionException("Sessão com XP negativo.");
        }
    }

    public static Session lesson(
            UUID id,
            UUID userId,
            UUID areaId,
            Level level,
            String competencySlug,
            List<UUID> questionIds,
            Instant now) {
        if (questionIds.size() < SessionAssembly.LESSON_MIN
                || questionIds.size() > SessionAssembly.LESSON_MAX) {
            throw new SessionException(
                    "Lição com " + questionIds.size() + " questões; precisa de 3 a 8.");
        }
        return new Session(
                id,
                userId,
                SessionFormat.LESSON,
                areaId,
                level,
                competencySlug,
                SessionStatus.IN_PROGRESS,
                null,
                now,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                numbered(questionIds),
                List.of());
    }

    public static Session simulation(
            UUID id,
            UUID userId,
            UUID areaId,
            Level level,
            SessionConfig config,
            List<UUID> questionIds,
            List<UUID> challengeIds,
            Instant now) {
        if (questionIds.size() != config.questionCount()
                || challengeIds.size() != config.challengeCount()) {
            throw new SessionException(
                    "A simulação precisa de "
                            + config.questionCount()
                            + " questões e "
                            + config.challengeCount()
                            + " desafios.");
        }
        distinct(challengeIds);
        return new Session(
                id,
                userId,
                SessionFormat.SIMULATION,
                areaId,
                level,
                null,
                SessionStatus.IN_PROGRESS,
                config.objectiveWeight(),
                now,
                now.plus(Duration.ofMinutes(config.durationMinutes())),
                null,
                null,
                null,
                null,
                0,
                null,
                numbered(questionIds),
                IntStream.range(0, challengeIds.size())
                        .mapToObj(i -> SessionChallenge.of(i + 1, challengeIds.get(i)))
                        .toList());
    }

    public boolean acceptsAt(Instant now) {
        return status == SessionStatus.IN_PROGRESS
                && (deadlineAt == null || !now.isAfter(deadlineAt.plus(GRACE)));
    }

    public Optional<SessionQuestion> question(int position) {
        return position >= 1 && position <= questions.size()
                ? Optional.of(questions.get(position - 1))
                : Optional.empty();
    }

    public Optional<SessionChallenge> challenge(int position) {
        return position >= 1 && position <= challenges.size()
                ? Optional.of(challenges.get(position - 1))
                : Optional.empty();
    }

    public boolean allAnswered() {
        return questions.stream().allMatch(SessionQuestion::answered);
    }

    public boolean reveals(SessionQuestion question) {
        return format == SessionFormat.LESSON
                ? question.answered()
                : status == SessionStatus.COMPLETED;
    }

    public Session answer(int position, UUID optionId, boolean correct, Instant now) {
        requireAccepting(now);
        SessionQuestion current =
                question(position)
                        .orElseThrow(
                                () ->
                                        new SessionException(
                                                "Questão fora da sessão: " + position + "."));
        if (format == SessionFormat.LESSON && current.answered()) {
            throw new SessionException("Na lição, a resposta é uma só.");
        }
        List<SessionQuestion> next = new ArrayList<>(questions);
        next.set(position - 1, current.answer(optionId, correct));
        return rebuild(status, null, null, null, null, 0, null, next, challenges);
    }

    public Session useSubmission(int position, int maxSubmissions, Instant now) {
        requireAccepting(now);
        SessionChallenge current =
                challenge(position)
                        .orElseThrow(
                                () ->
                                        new SessionException(
                                                "Desafio fora da sessão: " + position + "."));
        List<SessionChallenge> next = new ArrayList<>(challenges);
        next.set(position - 1, current.useSubmission(maxSubmissions));
        return rebuild(status, null, null, null, null, 0, null, questions, next);
    }

    public Session submit() {
        if (status != SessionStatus.IN_PROGRESS) {
            return this;
        }
        return rebuild(
                SessionStatus.SUBMITTED, null, null, null, null, 0, null, questions, challenges);
    }

    public Session complete(Map<Integer, CodeSubmission> bestByPosition, Instant now) {
        if (status != SessionStatus.SUBMITTED) {
            throw new SessionException(
                    "Só a sessão entregue é corrigida; esta está " + status + ".");
        }
        if (format == SessionFormat.LESSON) {
            Score score = Score.lesson(questions);
            return rebuild(
                    SessionStatus.COMPLETED,
                    now,
                    score.objectiveScore(),
                    null,
                    null,
                    0,
                    null,
                    questions,
                    challenges);
        }
        List<SessionChallenge> graded = new ArrayList<>();
        List<Optional<CodeSubmission>> bests = new ArrayList<>();
        for (SessionChallenge challenge : challenges) {
            CodeSubmission best = bestByPosition.get(challenge.position());
            if (best != null && !best.challengeId().equals(challenge.challengeId())) {
                throw new SessionException(
                        "Melhor envio de outro desafio na posição " + challenge.position() + ".");
            }
            bests.add(Optional.ofNullable(best));
            graded.add(challenge.withBest(best == null ? null : best.id()));
        }
        Score score = Score.simulation(questions, bests, objectiveWeight);
        return rebuild(
                SessionStatus.COMPLETED,
                now,
                score.objectiveScore(),
                score.challengeScore(),
                score.finalScore(),
                engaged() || !bestByPosition.isEmpty()
                        ? SessionXp.of(score.finalScore(), level)
                        : 0,
                null,
                questions,
                graded);
    }

    private boolean engaged() {
        return questions.stream().anyMatch(SessionQuestion::answered)
                || challenges.stream().anyMatch(challenge -> challenge.submissionsUsed() > 0);
    }

    public Session fail(String reason, Instant now) {
        if (status != SessionStatus.SUBMITTED) {
            throw new SessionException("Só a sessão entregue falha; esta está " + status + ".");
        }
        if (reason == null || reason.isBlank()) {
            throw new SessionException("Falha sem motivo.");
        }
        return rebuild(
                SessionStatus.FAILED, now, null, null, null, 0, reason, questions, challenges);
    }

    private void requireAccepting(Instant now) {
        if (!acceptsAt(now)) {
            throw new SessionException("A sessão não aceita mais respostas nem envios.");
        }
    }

    private Session rebuild(
            SessionStatus nextStatus,
            Instant finished,
            Double objective,
            Double challenge,
            Double result,
            int xp,
            String reason,
            List<SessionQuestion> nextQuestions,
            List<SessionChallenge> nextChallenges) {
        return new Session(
                id,
                userId,
                format,
                areaId,
                level,
                competencySlug,
                nextStatus,
                objectiveWeight,
                startedAt,
                deadlineAt,
                finished,
                objective,
                challenge,
                result,
                xp,
                reason,
                nextQuestions,
                nextChallenges);
    }

    private static List<SessionQuestion> numbered(List<UUID> questionIds) {
        distinct(questionIds);
        return IntStream.range(0, questionIds.size())
                .mapToObj(i -> SessionQuestion.of(i + 1, questionIds.get(i)))
                .toList();
    }

    private static void distinct(List<UUID> ids) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new SessionException("Item repetido na sessão.");
        }
    }

    private static void sequential(List<Integer> positions, String what) {
        for (int i = 0; i < positions.size(); i++) {
            if (positions.get(i) != i + 1) {
                throw new SessionException("Posições dos " + what + " fora de ordem.");
            }
        }
    }
}
