package br.com.devready.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.identity.exception.PasswordChangedMeanwhileException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

class ApiExceptionHandlerTest {

    @Test
    void integrityViolationIsAConflict() {
        ProblemDetail problem =
                new ApiExceptionHandler()
                        .integrityViolation(new DataIntegrityViolationException("users_email_key"));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflito");
        assertThat(problem.getDetail()).isEqualTo("Conflito ao gravar. Tente de novo.");
    }

    @Test
    void aPasswordChangedMeanwhileIsAConflict() {
        ProblemDetail problem =
                new ApiExceptionHandler()
                        .passwordChangedMeanwhile(new PasswordChangedMeanwhileException());

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Senha alterada");
        assertThat(problem.getDetail())
                .isEqualTo("Sua senha foi alterada por outro caminho. Entre de novo.");
    }
}
