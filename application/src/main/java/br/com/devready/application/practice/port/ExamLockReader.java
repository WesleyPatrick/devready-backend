package br.com.devready.application.practice.port;

import java.util.Map;
import java.util.UUID;

public interface ExamLockReader {
    Map<UUID, UUID> lockedChallenges(UUID userId);

    Map<UUID, UUID> lockedQuestions(UUID userId);
}
