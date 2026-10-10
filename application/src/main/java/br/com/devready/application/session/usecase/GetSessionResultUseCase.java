package br.com.devready.application.session.usecase;

import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.practice.dto.Placement;
import br.com.devready.application.practice.service.ProblemBoard;
import br.com.devready.application.session.dto.ChallengeCorrectionView;
import br.com.devready.application.session.dto.CompetencyResultView;
import br.com.devready.application.session.dto.QuestionCorrectionView;
import br.com.devready.application.session.dto.SessionResultView;
import br.com.devready.application.session.exception.SessionResultNotReadyException;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.service.SessionAccess;
import br.com.devready.application.session.service.SessionViews;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.session.Feedback;
import br.com.devready.domain.session.FeedbackItem;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionChallenge;
import br.com.devready.domain.session.SessionQuestion;
import br.com.devready.domain.session.SessionStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GetSessionResultUseCase {

    private final SessionRepository sessions;
    private final SessionPool pool;
    private final SubmissionRepository submissions;
    private final ProblemBoard board;

    public GetSessionResultUseCase(
            SessionRepository sessions,
            SessionPool pool,
            SubmissionRepository submissions,
            ProblemBoard board) {
        this.sessions = sessions;
        this.pool = pool;
        this.submissions = submissions;
        this.board = board;
    }

    public SessionResultView execute(UUID userId, UUID sessionId) {
        Session session = SessionAccess.owned(sessions, userId, sessionId);
        if (session.status() != SessionStatus.COMPLETED) {
            throw new SessionResultNotReadyException();
        }
        Function<Set<String>, Placement> placer = board.placer(userId);
        Map<UUID, Question> questionsById =
                pool
                        .questions(
                                session.questions().stream()
                                        .map(SessionQuestion::questionId)
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Question::id, Function.identity()));
        Map<UUID, Challenge> challengesById =
                pool
                        .challenges(
                                session.challenges().stream()
                                        .map(SessionChallenge::challengeId)
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Challenge::id, Function.identity()));
        List<FeedbackItem> items = new ArrayList<>();
        Map<String, Competency> bySlug = new HashMap<>();
        List<QuestionCorrectionView> questions = new ArrayList<>();
        for (SessionQuestion item : session.questions()) {
            Question question = SessionViews.required(questionsById, item.questionId());
            questions.add(
                    new QuestionCorrectionView(
                            item.position(),
                            question.slug(),
                            question.title(),
                            item.hit(),
                            item.optionId(),
                            question.correctOption().id(),
                            question.explanation()));
            items.add(feedbackItem(placer, question.tagSlugs(), item.hit(), bySlug));
        }
        List<ChallengeCorrectionView> challenges = new ArrayList<>();
        for (SessionChallenge item : session.challenges()) {
            Challenge challenge = SessionViews.required(challengesById, item.challengeId());
            Optional<CodeSubmission> best =
                    Optional.ofNullable(item.bestSubmissionId()).flatMap(submissions::findById);
            challenges.add(
                    new ChallengeCorrectionView(
                            item.position(),
                            challenge.slug(),
                            challenge.title(),
                            best.map(CodeSubmission::passedCount).orElse(0),
                            best.map(CodeSubmission::totalCount)
                                    .orElse(challenge.testCases().size()),
                            item.bestSubmissionId()));
            boolean hit = best.map(s -> s.status() == SubmissionStatus.PASSED).orElse(false);
            items.add(feedbackItem(placer, challenge.tagSlugs(), hit, bySlug));
        }
        List<CompetencyResultView> competencies =
                Feedback.of(items).stream()
                        .map(
                                feedback ->
                                        new CompetencyResultView(
                                                feedback.competencySlug() == null
                                                        ? null
                                                        : bySlug.get(feedback.competencySlug()),
                                                feedback))
                        .toList();
        return new SessionResultView(session, competencies, questions, challenges);
    }

    private static FeedbackItem feedbackItem(
            Function<Set<String>, Placement> placer,
            Set<String> tags,
            boolean hit,
            Map<String, Competency> bySlug) {
        Placement placement = placer.apply(tags);
        Competency competency = placement.competency();
        if (competency == null) {
            return new FeedbackItem(null, null, hit);
        }
        bySlug.putIfAbsent(competency.slug(), competency);
        return new FeedbackItem(competency.slug(), competency.name(), hit);
    }
}
