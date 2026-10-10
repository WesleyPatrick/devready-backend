package br.com.devready.domain.evaluation;

import br.com.devready.domain.catalog.Language;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CodeSubmission(
        UUID id,
        UUID userId,
        UUID challengeId,
        Language language,
        String sourceCode,
        SubmissionKind kind,
        SubmissionStatus status,
        int passedCount,
        int totalCount,
        String compileOutput,
        String errorMessage,
        int xpReward,
        int xpGained,
        Instant createdAt,
        Instant finishedAt,
        List<CaseResult> results,
        UUID sessionId) {

    public static final int MAX_SOURCE_CHARS = 65_536;

    public static final Duration STUCK_AFTER = Duration.ofMinutes(2);

    static final String STUCK_MESSAGE = "O envio não terminou a tempo. Tente de novo.";

    public CodeSubmission {
        if (id == null
                || userId == null
                || challengeId == null
                || language == null
                || kind == null
                || status == null
                || createdAt == null) {
            throw new EvaluationException(
                    "Envio sem id, pessoa, desafio, linguagem, tipo, situação ou hora.");
        }
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new EvaluationException("Envio sem código.");
        }
        if (sourceCode.length() > MAX_SOURCE_CHARS) {
            throw new EvaluationException("Código acima de 64 KB.");
        }
        if (totalCount < 1 || passedCount < 0 || passedCount > totalCount) {
            throw new EvaluationException(
                    "Contagem de casos inválida: " + passedCount + " de " + totalCount + ".");
        }
        if (xpReward < 0 || (kind == SubmissionKind.RUN && xpReward > 0)) {
            throw new EvaluationException("XP do envio inválido: rodar não vale XP.");
        }
        if (sessionId != null && xpReward > 0) {
            throw new EvaluationException("Envio de sessão não vale XP: a sessão dá o bônus dela.");
        }
        if (xpGained != 0 && (status != SubmissionStatus.PASSED || xpGained != xpReward)) {
            throw new EvaluationException("Só o envio aceito ganha XP, e o que ele vale.");
        }
        if (status.finished() != (finishedAt != null)) {
            throw new EvaluationException("Envio terminado precisa da hora do fim, e só ele.");
        }
        results = results == null ? List.of() : List.copyOf(results);
        if (results.size() > totalCount) {
            throw new EvaluationException("Mais resultados que casos no envio.");
        }
    }

    public static CodeSubmission queue(
            UUID id,
            UUID userId,
            UUID challengeId,
            Language language,
            String sourceCode,
            SubmissionKind kind,
            int totalCount,
            int xpReward,
            Instant now) {
        return new CodeSubmission(
                id,
                userId,
                challengeId,
                language,
                sourceCode,
                kind,
                SubmissionStatus.QUEUED,
                0,
                totalCount,
                null,
                null,
                xpReward,
                0,
                now,
                null,
                List.of(),
                null);
    }

    public static CodeSubmission queueInSession(
            UUID id,
            UUID userId,
            UUID challengeId,
            Language language,
            String sourceCode,
            SubmissionKind kind,
            int totalCount,
            UUID sessionId,
            Instant now) {
        if (sessionId == null) {
            throw new EvaluationException("Envio de sessão sem a sessão.");
        }
        return new CodeSubmission(
                id,
                userId,
                challengeId,
                language,
                sourceCode,
                kind,
                SubmissionStatus.QUEUED,
                0,
                totalCount,
                null,
                null,
                0,
                0,
                now,
                null,
                List.of(),
                sessionId);
    }

    public CodeSubmission start() {
        if (status != SubmissionStatus.QUEUED) {
            throw new EvaluationException(
                    "Só um envio na fila começa a rodar; este está " + status + ".");
        }
        return with(SubmissionStatus.RUNNING, 0, null, null, 0, null, List.of());
    }

    public CodeSubmission finish(Verdict verdict, boolean firstSolve, Instant now) {
        if (status != SubmissionStatus.RUNNING) {
            throw new EvaluationException("Só um envio rodando termina; este está " + status + ".");
        }
        int passed =
                (int)
                        verdict.results().stream()
                                .filter(result -> result.status() == CaseStatus.PASSED)
                                .count();
        int xp = verdict.status() == SubmissionStatus.PASSED && firstSolve ? xpReward : 0;
        return with(
                verdict.status(),
                passed,
                verdict.compileOutput(),
                verdict.errorMessage(),
                xp,
                now,
                verdict.results());
    }

    public CodeSubmission fail(String message, Instant now) {
        if (status.finished()) {
            throw new EvaluationException("Envio já terminado não falha de novo.");
        }
        return with(SubmissionStatus.ERROR, 0, null, Clip.of(message), 0, now, List.of());
    }

    public boolean stuckAt(Instant now) {
        return !status.finished() && now.isAfter(createdAt.plus(STUCK_AFTER));
    }

    public CodeSubmission expireAt(Instant now) {
        return stuckAt(now) ? fail(STUCK_MESSAGE, now) : this;
    }

    private CodeSubmission with(
            SubmissionStatus next,
            int passed,
            String compile,
            String error,
            int xp,
            Instant finished,
            List<CaseResult> caseResults) {
        return new CodeSubmission(
                id,
                userId,
                challengeId,
                language,
                sourceCode,
                kind,
                next,
                passed,
                totalCount,
                compile,
                error,
                xpReward,
                xp,
                createdAt,
                finished,
                caseResults,
                sessionId);
    }
}
