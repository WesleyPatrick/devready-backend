package br.com.devready.infrastructure.evaluation.messaging;

public final class EvaluationMessaging {

    public static final String EXCHANGE = "devready";
    public static final String QUEUE = "code-submissions";
    public static final String ROUTING_KEY = "submission.queued";
    public static final String DEAD_LETTER_QUEUE = "devready.dlq";

    private EvaluationMessaging() {}
}
