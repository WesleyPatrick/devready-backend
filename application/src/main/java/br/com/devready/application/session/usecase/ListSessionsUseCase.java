package br.com.devready.application.session.usecase;

import br.com.devready.application.session.dto.SessionSummaryView;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.service.SessionViews;
import java.util.List;
import java.util.UUID;

public class ListSessionsUseCase {

    private final SessionRepository sessions;
    private final SessionViews views;

    public ListSessionsUseCase(SessionRepository sessions, SessionViews views) {
        this.sessions = sessions;
        this.views = views;
    }

    public List<SessionSummaryView> execute(UUID userId) {
        return views.summaries(sessions.findByUser(userId));
    }
}
