package br.com.devready.application.practice.service;

import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.evaluation.port.SubmissionQueue;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.practice.dto.SubmissionTicket;
import br.com.devready.application.practice.exception.ChallengeNotFoundException;
import br.com.devready.application.practice.exception.LanguageNotOfferedException;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.practice.Xp;
import java.time.Instant;
import java.util.UUID;

public class SubmissionDesk {

    private final ChallengeRepository challenges;
    private final SubmissionRepository submissions;
    private final SubmissionQueue queue;
    private final IdGenerator ids;
    private final Clock clock;

    public SubmissionDesk(
            ChallengeRepository challenges,
            SubmissionRepository submissions,
            SubmissionQueue queue,
            IdGenerator ids,
            Clock clock) {
        this.challenges = challenges;
        this.submissions = submissions;
        this.queue = queue;
        this.ids = ids;
        this.clock = clock;
    }

    public Challenge activeChallenge(String slug) {
        return challenges
                .findBySlug(slug)
                .filter(Challenge::active)
                .orElseThrow(() -> new ChallengeNotFoundException(slug));
    }

    public SubmissionTicket queue(
            UUID userId, String slug, Language language, String sourceCode, SubmissionKind kind) {
        Challenge challenge = activeChallenge(slug);
        requireLanguage(challenge, language);
        return publish(
                CodeSubmission.queue(
                        ids.next(),
                        userId,
                        challenge.id(),
                        language,
                        sourceCode,
                        kind,
                        casesOf(challenge, kind),
                        kind == SubmissionKind.RUN
                                ? 0
                                : Xp.challengeFirstSolve(challenge.difficulty()),
                        clock.now()));
    }

    public SubmissionTicket queueInSession(
            UUID userId,
            Challenge challenge,
            Language language,
            String sourceCode,
            SubmissionKind kind,
            UUID sessionId) {
        requireLanguage(challenge, language);
        return publish(
                CodeSubmission.queueInSession(
                        ids.next(),
                        userId,
                        challenge.id(),
                        language,
                        sourceCode,
                        kind,
                        casesOf(challenge, kind),
                        sessionId,
                        clock.now()));
    }

    private static void requireLanguage(Challenge challenge, Language language) {
        if (!challenge.languages().containsKey(language)) {
            throw new LanguageNotOfferedException(language);
        }
    }

    private static int casesOf(Challenge challenge, SubmissionKind kind) {
        return kind == SubmissionKind.RUN
                ? challenge.visibleTestCases().size()
                : challenge.testCases().size();
    }

    private SubmissionTicket publish(CodeSubmission submission) {
        CodeSubmission queued = submissions.save(submission);
        queue.publish(queued.id());
        return new SubmissionTicket(queued.id(), queued.status());
    }

    public CodeSubmission current(CodeSubmission submission) {
        Instant now = clock.now();
        return submission.stuckAt(now) ? submissions.save(submission.expireAt(now)) : submission;
    }
}
