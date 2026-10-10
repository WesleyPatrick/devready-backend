package br.com.devready.infrastructure.practice.persistence;

import br.com.devready.application.practice.port.ChallengeAttemptReader;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.practice.ChallengeAttempt;
import br.com.devready.infrastructure.evaluation.persistence.AttemptRow;
import br.com.devready.infrastructure.evaluation.persistence.SpringDataCodeSubmissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaChallengeAttemptReader implements ChallengeAttemptReader {

    private final SpringDataCodeSubmissionRepository submissions;

    public JpaChallengeAttemptReader(SpringDataCodeSubmissionRepository submissions) {
        this.submissions = submissions;
    }

    @Override
    public List<ChallengeAttempt> findByUser(UUID userId) {
        return submissions.attemptsOfUser(userId, SubmissionKind.SUBMIT).stream()
                .map(JpaChallengeAttemptReader::toDomain)
                .toList();
    }

    @Override
    public List<ChallengeAttempt> findByChallenges(Collection<UUID> challengeIds) {
        if (challengeIds.isEmpty()) {
            return List.of();
        }
        return submissions.attemptsOnChallenges(challengeIds, SubmissionKind.SUBMIT).stream()
                .map(JpaChallengeAttemptReader::toDomain)
                .toList();
    }

    private static ChallengeAttempt toDomain(AttemptRow row) {
        return new ChallengeAttempt(
                row.userId(), row.challengeId(), row.status(), row.xpGained(), row.createdAt());
    }
}
