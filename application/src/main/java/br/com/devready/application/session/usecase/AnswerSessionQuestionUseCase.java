package br.com.devready.application.session.usecase;

import br.com.devready.application.practice.dto.AnswerResult;
import br.com.devready.application.practice.exception.OptionNotInQuestionException;
import br.com.devready.application.practice.usecase.AnswerQuestionUseCase;
import br.com.devready.application.session.dto.SessionAnswer;
import br.com.devready.application.session.dto.SessionQuestionView;
import br.com.devready.application.session.exception.ItemNotInSessionException;
import br.com.devready.application.session.exception.QuestionAlreadyAnsweredException;
import br.com.devready.application.session.exception.SessionClosedException;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.service.SessionAccess;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.Transactions;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionQuestion;
import br.com.devready.domain.session.SessionStatus;
import java.time.Instant;
import java.util.UUID;

public class AnswerSessionQuestionUseCase {

    private final SessionRepository sessions;
    private final SessionPool pool;
    private final AnswerQuestionUseCase answerQuestion;
    private final FinishSessionUseCase finish;
    private final Transactions transactions;
    private final Clock clock;

    public AnswerSessionQuestionUseCase(
            SessionRepository sessions,
            SessionPool pool,
            AnswerQuestionUseCase answerQuestion,
            FinishSessionUseCase finish,
            Transactions transactions,
            Clock clock) {
        this.sessions = sessions;
        this.pool = pool;
        this.answerQuestion = answerQuestion;
        this.finish = finish;
        this.transactions = transactions;
        this.clock = clock;
    }

    public SessionAnswer execute(UUID userId, UUID sessionId, int position, UUID optionId) {
        Session session = SessionAccess.owned(sessions, userId, sessionId);
        SessionQuestion item =
                session.question(position)
                        .orElseThrow(() -> new ItemNotInSessionException("questão", position));
        Instant now = clock.now();
        if (!session.acceptsAt(now)) {
            throw new SessionClosedException();
        }
        Question question = SessionAccess.questionOf(pool, item);
        QuestionOption option =
                question.options().stream()
                        .filter(candidate -> candidate.id().equals(optionId))
                        .findFirst()
                        .orElseThrow(OptionNotInQuestionException::new);
        if (session.format() == SessionFormat.SIMULATION) {
            Session answered = session.answer(position, optionId, option.correct(), now);
            if (!sessions.answer(sessionId, position, optionId, option.correct(), true)) {
                throw new SessionClosedException();
            }
            return new SessionAnswer(view(answered, position, question), 0, answered.status());
        }
        if (item.answered()) {
            throw new QuestionAlreadyAnsweredException();
        }
        Session answered = session.answer(position, optionId, option.correct(), now);
        AnswerResult result =
                transactions.inTransaction(
                        () -> {
                            if (!sessions.answer(
                                    sessionId, position, optionId, option.correct(), false)) {
                                throw sessions.findById(sessionId)
                                                .filter(
                                                        s ->
                                                                s.status()
                                                                        == SessionStatus
                                                                                .IN_PROGRESS)
                                                .isPresent()
                                        ? new QuestionAlreadyAnsweredException()
                                        : new SessionClosedException();
                            }
                            return answerQuestion.record(userId, question.slug(), optionId);
                        });
        Session after = sessions.findById(sessionId).orElseThrow();
        SessionStatus status =
                after.status() == SessionStatus.IN_PROGRESS && after.allAnswered()
                        ? finish.execute(userId, sessionId).status()
                        : after.status();
        return new SessionAnswer(view(answered, position, question), result.xpGained(), status);
    }

    private static SessionQuestionView view(Session session, int position, Question question) {
        return SessionQuestionView.of(session, session.question(position).orElseThrow(), question);
    }
}
