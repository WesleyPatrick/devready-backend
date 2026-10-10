package br.com.devready.infrastructure.session.persistence;

import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionFormat format;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Level level;

    @Column(name = "competency_slug", length = 50)
    private String competencySlug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "objective_weight")
    private Double objectiveWeight;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "objective_score")
    private Double objectiveScore;

    @Column(name = "challenge_score")
    private Double challengeScore;

    @Column(name = "final_score")
    private Double finalScore;

    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "session_questions", joinColumns = @JoinColumn(name = "session_id"))
    @BatchSize(size = 20)
    private List<SessionQuestionEmbeddable> questions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "session_challenges", joinColumns = @JoinColumn(name = "session_id"))
    @BatchSize(size = 20)
    private List<SessionChallengeEmbeddable> challenges = new ArrayList<>();

    protected SessionEntity() {}

    static SessionEntity from(Session session) {
        SessionEntity e = new SessionEntity();
        e.id = session.id();
        e.userId = session.userId();
        e.format = session.format();
        e.areaId = session.areaId();
        e.level = session.level();
        e.competencySlug = session.competencySlug();
        e.status = session.status();
        e.objectiveWeight = session.objectiveWeight();
        e.startedAt = session.startedAt();
        e.deadlineAt = session.deadlineAt();
        e.finishedAt = session.finishedAt();
        e.objectiveScore = session.objectiveScore();
        e.challengeScore = session.challengeScore();
        e.finalScore = session.finalScore();
        e.xpAwarded = session.xpAwarded();
        e.failureReason = session.failureReason();
        session.questions().forEach(q -> e.questions.add(SessionQuestionEmbeddable.from(q)));
        session.challenges().forEach(c -> e.challenges.add(SessionChallengeEmbeddable.from(c)));
        return e;
    }

    Session toDomain() {
        return new Session(
                id,
                userId,
                format,
                areaId,
                level,
                competencySlug,
                status,
                objectiveWeight,
                startedAt,
                deadlineAt,
                finishedAt,
                objectiveScore,
                challengeScore,
                finalScore,
                xpAwarded,
                failureReason,
                questions.stream()
                        .sorted(Comparator.comparingInt(SessionQuestionEmbeddable::position))
                        .map(SessionQuestionEmbeddable::toDomain)
                        .toList(),
                challenges.stream()
                        .sorted(Comparator.comparingInt(SessionChallengeEmbeddable::position))
                        .map(SessionChallengeEmbeddable::toDomain)
                        .toList());
    }
}
