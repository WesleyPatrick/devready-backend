package br.com.devready.infrastructure.practice.persistence;

import br.com.devready.application.career.port.QuestionProgressReader;
import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionStatus;
import br.com.devready.infrastructure.evaluation.persistence.SpringDataCodeSubmissionRepository;
import br.com.devready.infrastructure.session.persistence.SpringDataSessionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaQuestionProgressReader implements QuestionProgressReader {

    private final SpringDataAnswerRepository answers;
    private final SpringDataCodeSubmissionRepository submissions;
    private final SpringDataSessionRepository sessions;

    public JpaQuestionProgressReader(
            SpringDataAnswerRepository answers,
            SpringDataCodeSubmissionRepository submissions,
            SpringDataSessionRepository sessions) {
        this.answers = answers;
        this.submissions = submissions;
        this.sessions = sessions;
    }

    @Override
    public Map<String, QuestionProgress> answeredBy(UUID userId) {
        Map<String, QuestionProgress> progress = new HashMap<>();
        collect(answers.solvedBySlug(userId), progress);
        collect(submissions.solvedChallengesBySlug(userId), progress);
        sessions.questionSlugsRightIn(userId, SessionFormat.SIMULATION, SessionStatus.COMPLETED)
                .forEach(slug -> progress.put(slug, QuestionProgress.SOLVED));
        sessions.challengeSlugsPassedIn(
                        userId,
                        SessionStatus.COMPLETED,
                        SubmissionKind.SUBMIT,
                        SubmissionStatus.PASSED)
                .forEach(slug -> progress.put(slug, QuestionProgress.SOLVED));
        return progress;
    }

    private static void collect(List<Object[]> rows, Map<String, QuestionProgress> progress) {
        for (Object[] row : rows) {
            boolean solved = ((Number) row[1]).intValue() == 1;
            progress.merge(
                    (String) row[0],
                    solved ? QuestionProgress.SOLVED : QuestionProgress.ATTEMPTED,
                    (kept, other) ->
                            kept == QuestionProgress.SOLVED || other == QuestionProgress.SOLVED
                                    ? QuestionProgress.SOLVED
                                    : QuestionProgress.ATTEMPTED);
        }
    }
}
