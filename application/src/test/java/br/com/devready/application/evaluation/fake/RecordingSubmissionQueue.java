package br.com.devready.application.evaluation.fake;

import br.com.devready.application.evaluation.port.SubmissionQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecordingSubmissionQueue implements SubmissionQueue {
    private final List<UUID> published = new ArrayList<>();

    @Override
    public void publish(UUID submissionId) {
        published.add(submissionId);
    }

    public List<UUID> published() {
        return List.copyOf(published);
    }
}
