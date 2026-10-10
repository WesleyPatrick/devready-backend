package br.com.devready.infrastructure.catalog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "challenge_test_cases")
public class ChallengeTestCaseEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private ChallengeEntity challenge;

    @Column(nullable = false)
    private int position;

    @Column(name = "args_json", nullable = false, columnDefinition = "text")
    private String argsJson;

    @Column(name = "expected_json", nullable = false, columnDefinition = "text")
    private String expectedJson;

    @Column(nullable = false)
    private boolean visible;

    protected ChallengeTestCaseEntity() {}

    public ChallengeTestCaseEntity(
            UUID id,
            ChallengeEntity challenge,
            int position,
            String argsJson,
            String expectedJson,
            boolean visible) {
        this.id = id;
        this.challenge = challenge;
        this.position = position;
        this.argsJson = argsJson;
        this.expectedJson = expectedJson;
        this.visible = visible;
    }

    public UUID getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getArgsJson() {
        return argsJson;
    }

    public String getExpectedJson() {
        return expectedJson;
    }

    public boolean isVisible() {
        return visible;
    }
}
