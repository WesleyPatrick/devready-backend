package br.com.devready.application.practice.fake;

import br.com.devready.application.practice.port.ExamLockReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InMemoryExamLockReader implements ExamLockReader {

    private final Map<UUID, Map<UUID, UUID>> challengesByUser = new HashMap<>();
    private final Map<UUID, Map<UUID, UUID>> questionsByUser = new HashMap<>();

    @Override
    public Map<UUID, UUID> lockedChallenges(UUID userId) {
        return Map.copyOf(challengesByUser.getOrDefault(userId, Map.of()));
    }

    @Override
    public Map<UUID, UUID> lockedQuestions(UUID userId) {
        return Map.copyOf(questionsByUser.getOrDefault(userId, Map.of()));
    }

    public void lock(UUID user, UUID challengeId, UUID sessionId) {
        challengesByUser.computeIfAbsent(user, id -> new HashMap<>()).put(challengeId, sessionId);
    }

    public void lockQuestion(UUID user, UUID questionId, UUID sessionId) {
        questionsByUser.computeIfAbsent(user, id -> new HashMap<>()).put(questionId, sessionId);
    }

    public void unlockAll() {
        challengesByUser.clear();
        questionsByUser.clear();
    }
}
