package br.com.devready.application.session.usecase;

import br.com.devready.application.practice.dto.SubmissionView;
import br.com.devready.application.practice.usecase.GetSubmissionUseCase;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import java.util.UUID;

public class ViewSubmissionUseCase {

    private final GetSubmissionUseCase getSubmission;
    private final SessionRepository sessions;

    public ViewSubmissionUseCase(GetSubmissionUseCase getSubmission, SessionRepository sessions) {
        this.getSubmission = getSubmission;
        this.sessions = sessions;
    }

    public SubmissionView execute(UUID userId, UUID submissionId) {
        SubmissionView view = getSubmission.execute(userId, submissionId);
        CodeSubmission submission = view.submission();
        boolean sealed =
                submission.kind() == SubmissionKind.SUBMIT
                        && submission.sessionId() != null
                        && sessions.findById(submission.sessionId())
                                .map(session -> !session.status().closed())
                                .orElse(false);
        return sealed ? view.sealedView() : view;
    }
}
