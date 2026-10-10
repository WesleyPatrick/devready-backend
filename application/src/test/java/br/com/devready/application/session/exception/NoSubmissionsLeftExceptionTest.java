package br.com.devready.application.session.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoSubmissionsLeftExceptionTest {

    @Test
    void theMessageAgreesWithTheLimit() {
        assertThat(new NoSubmissionsLeftException(2, 1))
                .hasMessage("O único envio do desafio 2 já foi usado.");
        assertThat(new NoSubmissionsLeftException(1, 3))
                .hasMessage("Os 3 envios do desafio 1 já foram usados.");
    }
}
