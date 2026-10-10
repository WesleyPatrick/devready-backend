package br.com.devready.domain.session;

public enum FeedbackVerdict {
    STRONG,
    OK,
    GAP;

    public static FeedbackVerdict of(int hits, int total) {
        if (total < 1 || hits < 0 || hits > total) {
            throw new SessionException(
                    "Contagem de feedback inválida: " + hits + " de " + total + ".");
        }
        if (hits * 10 >= total * 7) {
            return STRONG;
        }
        return hits * 2 < total ? GAP : OK;
    }
}
