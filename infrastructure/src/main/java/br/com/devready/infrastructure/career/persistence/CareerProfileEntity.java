package br.com.devready.infrastructure.career.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "career_profiles")
public class CareerProfileEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "stack_id", nullable = false)
    private UUID stackId;

    @Column(nullable = false, length = 10)
    private String seniority;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "career_profile_companies", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    @Column(name = "company_id", nullable = false)
    private List<UUID> companyIds = new ArrayList<>();

    protected CareerProfileEntity() {}

    public CareerProfileEntity(UUID userId) {
        this.userId = userId;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<UUID> getCompanyIds() {
        return companyIds;
    }

    public void setCompanyIds(List<UUID> companyIds) {
        List<UUID> copy = List.copyOf(companyIds);
        this.companyIds.clear();
        this.companyIds.addAll(copy);
    }
}
