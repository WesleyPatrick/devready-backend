package br.com.devready.domain.session;

import static br.com.devready.domain.session.SessionFixtures.T0;
import static br.com.devready.domain.session.SessionFixtures.id;
import static br.com.devready.domain.session.SessionFixtures.queued;
import static br.com.devready.domain.session.SessionFixtures.run;
import static br.com.devready.domain.session.SessionFixtures.submit;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BestSubmissionTest {

    @Test
    void theOneWithMostPassedCasesWinsAndATieStaysWithTheEarlier() {
        CodeSubmission early = submit(id(11), SubmissionStatus.FAILED, 3, T0);
        CodeSubmission tie = submit(id(11), SubmissionStatus.FAILED, 3, T0.plusSeconds(10));
        CodeSubmission worse = submit(id(11), SubmissionStatus.FAILED, 1, T0.plusSeconds(20));

        assertThat(BestSubmission.of(List.of(worse, tie, early))).contains(early);
        CodeSubmission better = submit(id(11), SubmissionStatus.PASSED, 4, T0.plusSeconds(30));
        assertThat(BestSubmission.of(List.of(early, better))).contains(better);
    }

    @Test
    void runsAndUnfinishedSubmissionsNeverCount() {
        assertThat(BestSubmission.of(List.of(run(id(11), 2, T0), queued(id(11), T0)))).isEmpty();
        assertThat(BestSubmission.of(List.of())).isEmpty();
    }

    @Test
    void aSystemErrorCountsAsZeroCases() {
        CodeSubmission error = submit(id(11), SubmissionStatus.ERROR, 0, T0);

        assertThat(BestSubmission.of(List.of(error))).contains(error);
        assertThat(BestSubmission.ratio(Optional.of(error))).isEqualByComparingTo("0");
    }

    @Test
    void theRatioIsPassedOverTotalWithFourDecimals() {
        assertThat(
                        BestSubmission.ratio(
                                Optional.of(submit(id(11), SubmissionStatus.FAILED, 3, T0))))
                .isEqualByComparingTo(new BigDecimal("0.75"));
        assertThat(BestSubmission.ratio(Optional.empty())).isEqualByComparingTo("0");
    }
}
