package br.com.devready.application.session.usecase;

import br.com.devready.application.session.dto.SessionView;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.service.SessionAccess;
import br.com.devready.application.session.service.SessionViews;
import java.util.UUID;

public class GetSessionUseCase {

    private final SessionRepository sessions;
    private final SessionViews views;

    public GetSessionUseCase(SessionRepository sessions, SessionViews views) {
        this.sessions = sessions;
        this.views = views;
    }

    public SessionView execute(UUID userId, UUID sessionId) {
        return views.view(SessionAccess.owned(sessions, userId, sessionId));
    }
}
