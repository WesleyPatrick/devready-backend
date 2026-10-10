package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.domain.catalog.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "challenge_languages")
public class ChallengeLanguageEntity {
    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private ChallengeEntity challenge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    @Column(name = "function_name", nullable = false, length = 80)
    private String functionName;

    @Column(name = "starter_code", nullable = false, columnDefinition = "text")
    private String starterCode;

    @Column(name = "reference_solution", columnDefinition = "text")
    private String referenceSolution;

    protected ChallengeLanguageEntity() {}

    public ChallengeLanguageEntity(
            UUID id,
            ChallengeEntity challenge,
            Language language,
            String functionName,
            String starterCode,
            String referenceSolution) {
        this.id = id;
        this.challenge = challenge;
        this.language = language;
        this.functionName = functionName;
        this.starterCode = starterCode;
        this.referenceSolution = referenceSolution;
    }

    public Language getLanguage() {
        return language;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getStarterCode() {
        return starterCode;
    }

    public String getReferenceSolution() {
        return referenceSolution;
    }
}
