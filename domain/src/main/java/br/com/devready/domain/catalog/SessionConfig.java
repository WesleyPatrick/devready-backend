package br.com.devready.domain.catalog;

public record SessionConfig(
        int questionCount, int challengeCount, int durationMinutes, double objectiveWeight) {
    public SessionConfig {
        if (questionCount < 0 || challengeCount < 0) {
            throw new CatalogException("Quantidades não podem ser negativas.");
        }
        if (questionCount + challengeCount == 0) {
            throw new CatalogException("A sessão precisa de ao menos uma questão ou desafio.");
        }
        if (durationMinutes <= 0) {
            throw new CatalogException("A duração precisa ser positiva.");
        }
        if (objectiveWeight < 0 || objectiveWeight > 1) {
            throw new CatalogException("O peso das objetivas fica entre 0 e 1.");
        }
    }
}
