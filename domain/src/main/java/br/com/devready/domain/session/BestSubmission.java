package br.com.devready.domain.session;

import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public final class BestSubmission {

    private static final Comparator<CodeSubmission> ORDER =
            Comparator.comparingInt(CodeSubmission::passedCount)
                    .reversed()
                    .thenComparing(CodeSubmission::createdAt)
                    .thenComparing(CodeSubmission::id);

    private BestSubmission() {}

    public static Optional<CodeSubmission> of(Collection<CodeSubmission> toOneChallenge) {
        return toOneChallenge.stream()
                .filter(s -> s.kind() == SubmissionKind.SUBMIT && s.status().finished())
                .min(ORDER);
    }

    public static BigDecimal ratio(Optional<CodeSubmission> best) {
        return best.map(
                        s ->
                                BigDecimal.valueOf(s.passedCount())
                                        .divide(
                                                BigDecimal.valueOf(s.totalCount()),
                                                Score.SCALE,
                                                RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }
}
