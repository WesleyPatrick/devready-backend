package br.com.devready.domain.career;

public record TrackUnit(String competencySlug, double weight, Requirement requirement, int jobs) {
    public TrackUnit {
        Names.slug(competencySlug, "competência da trilha");
        if (Double.isNaN(weight) || weight < 0 || weight > 1) {
            throw new CareerException(
                    "Peso fora de 0 a 1 na competência " + competencySlug + ": " + weight + ".");
        }
        if (requirement == null) {
            throw new CareerException(
                    "Competência " + competencySlug + " sem exigência (obrigatoria ou desejavel).");
        }
        if (jobs < 0) {
            throw new CareerException("Vagas negativas na competência " + competencySlug + ".");
        }
    }
}
