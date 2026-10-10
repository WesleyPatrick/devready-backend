package br.com.devready.infrastructure.career.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class TrackUnitEmbeddable {
    @Column(name = "competency_id", nullable = false)
    private UUID competencyId;

    @Column(nullable = false)
    private double weight;

    @Column(nullable = false, length = 12)
    private String requirement;

    @Column(nullable = false)
    private int jobs;

    protected TrackUnitEmbeddable() {}

    public TrackUnitEmbeddable(UUID competencyId, double weight, String requirement, int jobs) {
        this.competencyId = competencyId;
        this.weight = weight;
        this.requirement = requirement;
        this.jobs = jobs;
    }

    public UUID getCompetencyId() {
        return competencyId;
    }

    public double getWeight() {
        return weight;
    }

    public String getRequirement() {
        return requirement;
    }

    public int getJobs() {
        return jobs;
    }
}
