package br.com.devready.infrastructure.practice.persistence;

import br.com.devready.application.practice.port.ExamLockReader;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionStatus;
import br.com.devready.infrastructure.session.persistence.SpringDataSessionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaExamLockReader implements ExamLockReader {

    private static final List<SessionStatus> OPEN =
            List.of(SessionStatus.IN_PROGRESS, SessionStatus.SUBMITTED);

    private final SpringDataSessionRepository sessions;

    public JpaExamLockReader(SpringDataSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public Map<UUID, UUID> lockedChallenges(UUID userId) {
        return bySession(sessions.challengesInOpenSessions(userId, SessionFormat.SIMULATION, OPEN));
    }

    @Override
    public Map<UUID, UUID> lockedQuestions(UUID userId) {
        return bySession(sessions.questionsInOpenSessions(userId, SessionFormat.SIMULATION, OPEN));
    }

    private static Map<UUID, UUID> bySession(List<Object[]> rows) {
        Map<UUID, UUID> locked = new HashMap<>();
        for (Object[] row : rows) {
            UUID problem = (UUID) row[0];
            UUID session = (UUID) row[1];
            if (row[2] == SessionStatus.IN_PROGRESS) {
                locked.put(problem, session);
            } else {
                locked.putIfAbsent(problem, session);
            }
        }
        return locked;
    }
}
