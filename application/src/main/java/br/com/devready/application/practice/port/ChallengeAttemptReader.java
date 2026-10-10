package br.com.devready.application.practice.port;

import br.com.devready.domain.practice.ChallengeAttempt;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChallengeAttemptReader {
    List<ChallengeAttempt> findByUser(UUID userId);

    List<ChallengeAttempt> findByChallenges(Collection<UUID> challengeIds);
}
