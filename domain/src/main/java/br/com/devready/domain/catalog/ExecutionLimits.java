package br.com.devready.domain.catalog;

public record ExecutionLimits(int timeLimitMs, int memoryLimitKb) {
    public ExecutionLimits {
        if (timeLimitMs <= 0 || memoryLimitKb <= 0) {
            throw new CatalogException("Limites de execução precisam ser positivos.");
        }
    }
}
