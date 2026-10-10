package br.com.devready.application.session.port;

import br.com.devready.domain.session.Session;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {

    Session create(Session session);

    Optional<Session> findById(UUID id);

    Optional<Session> findOpen(UUID userId);

    List<Session> findByUser(UUID userId);

    List<UUID> findSubmittedIds();

    boolean answer(UUID sessionId, int position, UUID optionId, boolean correct, boolean replace);

    boolean useSubmission(UUID sessionId, int position, int maxSubmissions);

    boolean submit(UUID sessionId);

    int expireDue(Instant cutoff);

    boolean complete(Session completed);

    boolean fail(Session failed);
}
