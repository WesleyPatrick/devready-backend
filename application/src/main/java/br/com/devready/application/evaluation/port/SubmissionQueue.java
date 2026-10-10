package br.com.devready.application.evaluation.port;

import java.util.UUID;

public interface SubmissionQueue {
    void publish(UUID submissionId);
}
