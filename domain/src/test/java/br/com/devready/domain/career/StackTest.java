package br.com.devready.domain.career;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StackTest {

    @Test
    void requiresSlugNameAndArea() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new Stack(id, "Java Spring", "Java e Spring", "backend"))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new Stack(id, "java-spring", "", "backend"))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new Stack(id, "java-spring", "Java e Spring", null))
                .isInstanceOf(CareerException.class);
    }
}
