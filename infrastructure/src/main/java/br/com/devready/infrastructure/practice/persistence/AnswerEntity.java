package br.com.devready.infrastructure.practice.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "answers")
public class AnswerEntity {
    @Id private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "option_id", nullable = false)
    private UUID optionId;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "xp_gained", nullable = false)
    private int xpGained;

    @Column(name = "answered_at", nullable = false)
    private Instant answeredAt;

    protected AnswerEntity() {}

    public AnswerEntity(
            UUID id,
            UUID userId,
            UUID questionId,
            UUID optionId,
            boolean correct,
            int xpGained,
            Instant answeredAt) {
        this.id = id;
        this.userId = userId;
        this.questionId = questionId;
        this.optionId = optionId;
        this.correct = correct;
        this.xpGained = xpGained;
        this.answeredAt = answeredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public UUID getOptionId() {
        return optionId;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getXpGained() {
        return xpGained;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}
