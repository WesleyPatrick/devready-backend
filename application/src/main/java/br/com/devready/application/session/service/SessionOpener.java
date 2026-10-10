package br.com.devready.application.session.service;

import br.com.devready.application.session.exception.OpenSessionConflictException;
import br.com.devready.application.session.exception.SessionInProgressException;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.usecase.FinishSessionUseCase;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionFormat;

public class SessionOpener {

    private final SessionRepository sessions;
    private final FinishSessionUseCase finish;

    public SessionOpener(SessionRepository sessions, FinishSessionUseCase finish) {
        this.sessions = sessions;
        this.finish = finish;
    }

    public Session open(Session fresh) {
        sessions.findOpen(fresh.userId()).ifPresent(this::makeRoom);
        try {
            return sessions.create(fresh);
        } catch (OpenSessionConflictException e) {
            Session open = sessions.findOpen(fresh.userId()).orElseThrow(() -> e);
            throw new SessionInProgressException(open.id(), open.format());
        }
    }

    private void makeRoom(Session open) {
        if (open.format() == SessionFormat.SIMULATION) {
            throw new SessionInProgressException(open.id(), open.format());
        }
        finish.execute(open.userId(), open.id());
    }
}
