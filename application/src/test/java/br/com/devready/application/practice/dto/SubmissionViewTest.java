package br.com.devready.application.practice.dto;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.evaluation.Verdict;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmissionViewTest {

    private static final Instant NOW = Instant.ofEpochSecond(1_700_000_000L);

    @Test
    void aSealedViewOfACompileErrorCarriesNoCompilerOutput() {
        CodeSubmission failed =
                finished(new Verdict(SubmissionStatus.COMPILE_ERROR, List.of(), "erro: ';'", null));
        assertThat(failed.compileOutput()).isNotNull();

        SubmissionView sealed = view(failed).sealedView();

        assertThat(sealed.sealed()).isTrue();
        assertThat(sealed.cases()).isEmpty();
        assertThat(sealed.submission().status()).isEqualTo(SubmissionStatus.COMPILE_ERROR);
        assertThat(sealed.submission().compileOutput()).isNull();
        assertThat(sealed.submission().errorMessage()).isNull();
    }

    @Test
    void aSealedViewOfARuntimeErrorCarriesNoErrorMessage() {
        CodeSubmission crashed =
                finished(
                        new Verdict(
                                SubmissionStatus.RUNTIME_ERROR,
                                List.of(),
                                null,
                                "stderr com os argumentos de um caso oculto"));
        assertThat(crashed.errorMessage()).isNotNull();

        SubmissionView sealed = view(crashed).sealedView();

        assertThat(sealed.submission().errorMessage()).isNull();
        assertThat(sealed.submission().compileOutput()).isNull();
        assertThat(sealed.submission().totalCount()).isEqualTo(crashed.totalCount());
        assertThat(sealed.submission().id()).isEqualTo(crashed.id());
    }

    private static SubmissionView view(CodeSubmission submission) {
        return new SubmissionView(submission, "c-sql", List.of(), false);
    }

    private static CodeSubmission finished(Verdict verdict) {
        return CodeSubmission.queueInSession(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Language.JAVA,
                        "x",
                        SubmissionKind.SUBMIT,
                        3,
                        UUID.randomUUID(),
                        NOW)
                .start()
                .finish(verdict, false, NOW);
    }
}
