package br.com.devready.api.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.devready.application.identity.usecase.BootstrapAdminUseCase;
import br.com.devready.domain.identity.WeakPasswordException;
import org.junit.jupiter.api.Test;

class AdminBootstrapRunnerTest {

    @Test
    void weakAdminPasswordStopsTheStartupWithAClearMessage() {
        BootstrapAdminUseCase useCase = mock(BootstrapAdminUseCase.class);
        when(useCase.execute("admin@devready.local", "troque"))
                .thenThrow(new WeakPasswordException());
        var runner =
                new AdminBootstrapRunner(
                        useCase, new AdminProperties("admin@devready.local", "troque"));

        assertThatThrownBy(() -> runner.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEVREADY_ADMIN_PASSWORD")
                .hasMessageContaining("pelo menos 8 caracteres");
    }
}
