package br.com.devready.application.practice.service;

import br.com.devready.application.practice.exception.ChallengeInExamException;
import br.com.devready.application.practice.exception.QuestionInExamException;
import br.com.devready.application.practice.port.ExamLockReader;
import java.util.UUID;

public final class ExamGuard {

    private ExamGuard() {}

    public static void requireChallengeFree(ExamLockReader locks, UUID userId, UUID challengeId) {
        UUID session = locks.lockedChallenges(userId).get(challengeId);
        if (session != null) {
            throw new ChallengeInExamException(session);
        }
    }

    public static void requireQuestionFree(ExamLockReader locks, UUID userId, UUID questionId) {
        UUID session = locks.lockedQuestions(userId).get(questionId);
        if (session != null) {
            throw new QuestionInExamException(session);
        }
    }
}
