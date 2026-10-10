package br.com.devready.domain.catalog;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SessionConfigTest {

    @Test
    void acceptsTheDefaultBackendConfiguration() {
        assertThatCode(() -> new SessionConfig(8, 2, 45, 0.5)).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptySessionsBadDurationAndWeightOutsideZeroOne() {
        assertThatThrownBy(() -> new SessionConfig(0, 0, 45, 0.5))
                .isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> new SessionConfig(8, 2, 0, 0.5))
                .isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> new SessionConfig(8, 2, 45, 1.5))
                .isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> new SessionConfig(-1, 2, 45, 0.5))
                .isInstanceOf(CatalogException.class);
    }
}
