package br.com.devready.infrastructure.practice.persistence;

import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.practice.CompletedSimulation;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionStatus;
import br.com.devready.infrastructure.session.persistence.SpringDataSessionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaSimulationRecordReader implements SimulationRecordReader {

    private final SpringDataSessionRepository sessions;

    public JpaSimulationRecordReader(SpringDataSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public List<CompletedSimulation> completedBy(UUID userId) {
        return sessions
                .simulationsOf(userId, SessionFormat.SIMULATION, SessionStatus.COMPLETED)
                .stream()
                .map(row -> new CompletedSimulation(row.finishedAt(), row.xpAwarded()))
                .toList();
    }

    @Override
    public Set<UUID> questionsSolvedBy(UUID userId) {
        return Set.copyOf(
                sessions.questionsRightIn(
                        userId, SessionFormat.SIMULATION, SessionStatus.COMPLETED));
    }

    @Override
    public Set<UUID> challengesSolvedBy(UUID userId) {
        return Set.copyOf(
                sessions.challengesPassedIn(
                        userId,
                        SessionStatus.COMPLETED,
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.PASSED));
    }
}
