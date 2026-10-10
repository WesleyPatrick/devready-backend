package br.com.devready.application.session.service;

import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.application.evaluation.port.LanguageRuntimes;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.practice.dto.ChallengeLanguageView;
import br.com.devready.application.session.dto.SessionChallengeView;
import br.com.devready.application.session.dto.SessionQuestionView;
import br.com.devready.application.session.dto.SessionSummaryView;
import br.com.devready.application.session.dto.SessionView;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.session.BestSubmission;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionChallenge;
import br.com.devready.domain.session.SessionException;
import br.com.devready.domain.session.SessionQuestion;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SessionViews {

    private final SessionPool pool;
    private final SubmissionRepository submissions;
    private final CompetencyRepository competencies;
    private final LanguageRuntimes runtimes;

    public SessionViews(
            SessionPool pool,
            SubmissionRepository submissions,
            CompetencyRepository competencies,
            LanguageRuntimes runtimes) {
        this.pool = pool;
        this.submissions = submissions;
        this.competencies = competencies;
        this.runtimes = runtimes;
    }

    public SessionView view(Session session) {
        Map<UUID, Question> questions =
                pool
                        .questions(
                                session.questions().stream()
                                        .map(SessionQuestion::questionId)
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Question::id, Function.identity()));
        Map<UUID, Challenge> challenges =
                pool
                        .challenges(
                                session.challenges().stream()
                                        .map(SessionChallenge::challengeId)
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Challenge::id, Function.identity()));
        List<CodeSubmission> submits =
                session.challenges().isEmpty()
                        ? List.of()
                        : submissions.findBySession(session.id()).stream()
                                .filter(s -> s.kind() == SubmissionKind.SUBMIT)
                                .toList();
        return new SessionView(
                session,
                areaOf(session),
                competencyOf(session),
                session.questions().stream()
                        .map(
                                item ->
                                        SessionQuestionView.of(
                                                session,
                                                item,
                                                required(questions, item.questionId())))
                        .toList(),
                session.challenges().stream()
                        .map(
                                item ->
                                        challengeView(
                                                item,
                                                required(challenges, item.challengeId()),
                                                submits))
                        .toList());
    }

    public List<SessionSummaryView> summaries(List<Session> sessions) {
        Map<UUID, Area> areas = new HashMap<>();
        Map<String, Competency> bySlug =
                sessions.stream().anyMatch(session -> session.competencySlug() != null)
                        ? competencies.findAll().stream()
                                .collect(Collectors.toMap(Competency::slug, Function.identity()))
                        : Map.of();
        return sessions.stream()
                .map(
                        session ->
                                new SessionSummaryView(
                                        session,
                                        areas.computeIfAbsent(
                                                session.areaId(), id -> areaOf(session)),
                                        session.competencySlug() == null
                                                ? null
                                                : bySlug.get(session.competencySlug())))
                .toList();
    }

    private SessionChallengeView challengeView(
            SessionChallenge item, Challenge challenge, List<CodeSubmission> submits) {
        List<CodeSubmission> mine =
                submits.stream().filter(s -> s.challengeId().equals(item.challengeId())).toList();
        UUID best =
                item.bestSubmissionId() != null
                        ? item.bestSubmissionId()
                        : BestSubmission.of(mine).map(CodeSubmission::id).orElse(null);
        UUID last =
                mine.stream()
                        .max(
                                Comparator.comparing(CodeSubmission::createdAt)
                                        .thenComparing(CodeSubmission::id))
                        .map(CodeSubmission::id)
                        .orElse(null);
        List<ChallengeLanguageView> languages = ChallengeLanguageView.listOf(challenge, runtimes);
        return new SessionChallengeView(
                item.position(),
                challenge.slug(),
                challenge.title(),
                challenge.difficulty(),
                challenge.statement(),
                languages,
                challenge.visibleTestCases().stream()
                        .sorted(Comparator.comparingInt(TestCase::position))
                        .toList(),
                challenge.limits(),
                item.submissionsUsed(),
                item.submissionsLeft(challenge.maxSubmissions()),
                best,
                last);
    }

    private Area areaOf(Session session) {
        return pool.area(session.areaId())
                .orElseThrow(() -> new SessionException("Área da sessão sumiu do catálogo."));
    }

    private Competency competencyOf(Session session) {
        if (session.competencySlug() == null) {
            return null;
        }
        return competencies.findAll().stream()
                .filter(c -> c.slug().equals(session.competencySlug()))
                .findFirst()
                .orElse(null);
    }

    public static <T> T required(Map<UUID, T> items, UUID id) {
        T item = items.get(id);
        if (item == null) {
            throw new SessionException("Item da sessão sumiu do catálogo: " + id + ".");
        }
        return item;
    }
}
