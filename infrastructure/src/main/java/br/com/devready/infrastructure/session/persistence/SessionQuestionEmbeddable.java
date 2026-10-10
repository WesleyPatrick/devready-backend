package br.com.devready.infrastructure.session.persistence;

import br.com.devready.domain.session.SessionQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class SessionQuestionEmbeddable {

    @Column(nullable = false)
    private int position;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "option_id")
    private UUID optionId;

    @Column(name = "correct")
    private Boolean correct;

    protected SessionQuestionEmbeddable() {}

    static SessionQuestionEmbeddable from(SessionQuestion question) {
        SessionQuestionEmbeddable e = new SessionQuestionEmbeddable();
        e.position = question.position();
        e.questionId = question.questionId();
        e.optionId = question.optionId();
        e.correct = question.correct();
        return e;
    }

    int position() {
        return position;
    }

    SessionQuestion toDomain() {
        return new SessionQuestion(position, questionId, optionId, correct);
    }
}
