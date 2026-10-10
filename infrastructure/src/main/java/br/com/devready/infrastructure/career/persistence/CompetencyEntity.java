package br.com.devready.infrastructure.career.persistence;

import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.infrastructure.catalog.persistence.TagEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "competencies")
public class CompetencyEntity {
    @Id private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, columnDefinition = "text")
    private String summary = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @ManyToMany
    @BatchSize(size = 50)
    @JoinTable(
            name = "competency_tags",
            joinColumns = @JoinColumn(name = "competency_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TagEntity> tags = new HashSet<>();

    @ElementCollection
    @BatchSize(size = 50)
    @CollectionTable(name = "materials", joinColumns = @JoinColumn(name = "competency_id"))
    @OrderColumn(name = "position")
    private List<MaterialEmbeddable> materials = new ArrayList<>();

    protected CompetencyEntity() {}

    public CompetencyEntity(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Set<TagEntity> getTags() {
        return tags;
    }

    public List<MaterialEmbeddable> getMaterials() {
        return materials;
    }

    public void setMaterials(List<MaterialEmbeddable> materials) {
        List<MaterialEmbeddable> copy = List.copyOf(materials);
        this.materials.clear();
        this.materials.addAll(copy);
    }
}
