package br.com.devready.infrastructure.career.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "tracks")
public class TrackEntity {
    @Id private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "area_slug", nullable = false, length = 50)
    private String areaSlug;

    @Column(name = "stack_id")
    private UUID stackId;

    @Column(length = 10)
    private String seniority;

    @Column(nullable = false)
    private int jobs;

    @Column(nullable = false)
    private boolean sample;

    @ElementCollection
    @BatchSize(size = 50)
    @CollectionTable(name = "track_units", joinColumns = @JoinColumn(name = "track_id"))
    @OrderColumn(name = "position")
    private List<TrackUnitEmbeddable> units = new ArrayList<>();

    protected TrackEntity() {}

    public TrackEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getAreaSlug() {
        return areaSlug;
    }

    public void setAreaSlug(String areaSlug) {
        this.areaSlug = areaSlug;
    }

    public UUID getStackId() {
        return stackId;
    }

    public void setStackId(UUID stackId) {
        this.stackId = stackId;
    }

    public String getSeniority() {
        return seniority;
    }

    public void setSeniority(String seniority) {
        this.seniority = seniority;
    }

    public int getJobs() {
        return jobs;
    }

    public void setJobs(int jobs) {
        this.jobs = jobs;
    }

    public void setSample(boolean sample) {
        this.sample = sample;
    }

    public List<TrackUnitEmbeddable> getUnits() {
        return units;
    }

    public void setUnits(List<TrackUnitEmbeddable> units) {
        List<TrackUnitEmbeddable> copy = List.copyOf(units);
        this.units.clear();
        this.units.addAll(copy);
    }
}
