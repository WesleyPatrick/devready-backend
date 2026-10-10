package br.com.devready.api.identity.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemporaryPasswordResponseTest {

    @Test
    void theTextOfTheObjectHidesThePassword() {
        TemporaryPasswordResponse response = new TemporaryPasswordResponse("q7HcV2mR9xTb4LwZ");

        assertThat(response.toString())
                .doesNotContain("q7HcV2mR9xTb4LwZ")
                .contains("temporaryPassword");
        assertThat(response.temporaryPassword()).isEqualTo("q7HcV2mR9xTb4LwZ");
    }
}
