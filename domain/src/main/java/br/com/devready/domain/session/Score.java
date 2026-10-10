package br.com.devready.domain.session;

import br.com.devready.domain.evaluation.CodeSubmission;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public record Score(double objectiveScore, Double challengeScore, Double finalScore) {

    public static final int SCALE = 4;

    public static Score lesson(List<SessionQuestion> questions) {
        return new Score(objective(questions).doubleValue(), null, null);
    }

    public static Score simulation(
            List<SessionQuestion> questions,
            List<Optional<CodeSubmission>> bestPerChallenge,
            double objectiveWeight) {
        BigDecimal objective = objective(questions);
        if (bestPerChallenge.isEmpty()) {
            return new Score(objective.doubleValue(), null, objective.doubleValue());
        }
        BigDecimal sum =
                bestPerChallenge.stream()
                        .map(BestSubmission::ratio)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal challenge =
                sum.divide(
                        BigDecimal.valueOf(bestPerChallenge.size()), SCALE, RoundingMode.HALF_UP);
        BigDecimal weight = BigDecimal.valueOf(objectiveWeight);
        BigDecimal result =
                weight.multiply(objective)
                        .add(BigDecimal.ONE.subtract(weight).multiply(challenge))
                        .setScale(SCALE, RoundingMode.HALF_UP);
        return new Score(objective.doubleValue(), challenge.doubleValue(), result.doubleValue());
    }

    private static BigDecimal objective(List<SessionQuestion> questions) {
        if (questions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long hits = questions.stream().filter(SessionQuestion::hit).count();
        return BigDecimal.valueOf(hits)
                .divide(BigDecimal.valueOf(questions.size()), SCALE, RoundingMode.HALF_UP);
    }
}
