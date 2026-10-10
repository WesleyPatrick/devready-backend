package br.com.devready.application.session.service;

import br.com.devready.application.session.exception.SessionNotFoundException;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionChallenge;
import br.com.devready.domain.session.SessionException;
import br.com.devready.domain.session.SessionQuestion;
import java.util.List;
import java.util.UUID;

public final class SessionAccess {

    private SessionAccess() {}

    public static Session owned(SessionRepository sessions, UUID userId, UUID sessionId) {
        return sessions.findById(sessionId)
                .filter(session -> session.userId().equals(userId))
                .orElseThrow(SessionNotFoundException::new);
    }

    public static Question questionOf(SessionPool pool, SessionQuestion item) {
        return pool.questions(List.of(item.questionId())).stream()
                .findFirst()
                .orElseThrow(() -> new SessionException("Questão da sessão sumiu do catálogo."));
    }

    public static Challenge challengeOf(SessionPool pool, SessionChallenge item) {
        return pool.challenges(List.of(item.challengeId())).stream()
                .findFirst()
                .orElseThrow(() -> new SessionException("Desafio da sessão sumiu do catálogo."));
    }
}
