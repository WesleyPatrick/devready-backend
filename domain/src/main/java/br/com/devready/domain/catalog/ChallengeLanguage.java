package br.com.devready.domain.catalog;

public record ChallengeLanguage(
        Language language, String functionName, String starterCode, String referenceSolution) {
    public ChallengeLanguage {
        if (language == null) {
            throw new CatalogException("Linguagem obrigatória.");
        }
        Text.require(functionName, "nome da função");
        Text.require(starterCode, "código inicial");
    }
}
