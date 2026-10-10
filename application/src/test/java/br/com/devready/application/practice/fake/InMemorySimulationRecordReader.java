package br.com.devready.application.practice.fake;

import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.domain.practice.CompletedSimulation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InMemorySimulationRecordReader implements SimulationRecordReader {

    private final Map<UUID, List<CompletedSimulation>> completed = new HashMap<>();
    private final Map<UUID, Set<UUID>> questions = new HashMap<>();
    private final Map<UUID, Set<UUID>> challenges = new HashMap<>();

    @Override
    public List<CompletedSimulation> completedBy(UUID userId) {
        return List.copyOf(completed.getOrDefault(userId, List.of()));
    }

    @Override
    public Set<UUID> questionsSolvedBy(UUID userId) {
        return Set.copyOf(questions.getOrDefault(userId, Set.of()));
    }

    @Override
    public Set<UUID> challengesSolvedBy(UUID userId) {
        return Set.copyOf(challenges.getOrDefault(userId, Set.of()));
    }

    public void complete(UUID user, Instant finishedAt, int xp) {
        completed
                .computeIfAbsent(user, id -> new ArrayList<>())
                .add(new CompletedSimulation(finishedAt, xp));
    }

    public void solveQuestion(UUID user, UUID questionId) {
        questions.computeIfAbsent(user, id -> new HashSet<>()).add(questionId);
    }

    public void solveChallenge(UUID user, UUID challengeId) {
        challenges.computeIfAbsent(user, id -> new HashSet<>()).add(challengeId);
    }
}
