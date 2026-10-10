package br.com.devready.application.session.usecase;

import br.com.devready.application.session.dto.SessionView;
import br.com.devready.application.session.exception.SessionNotFoundException;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.service.SessionViews;
import java.util.UUID;

public class GetCurrentSessionUseCase {

    private final SessionRepository sessions;
    private final SessionViews views;

    public GetCurrentSessionUseCase(SessionRepository sessions, SessionViews views) {
        this.sessions = sessions;
        this.views = views;
    }

    public SessionView execute(UUID userId) {
        return sessions.findOpen(userId)
                .map(views::view)
                .orElseThrow(SessionNotFoundException::new);
    }
}
