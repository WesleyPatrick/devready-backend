package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.BootstrapAdminOutcome;
import br.com.devready.application.identity.fake.IdentityFixtures;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.identity.WeakPasswordException;
import org.junit.jupiter.api.Test;

class BootstrapAdminUseCaseTest {

    private final IdentityFixtures f = new IdentityFixtures();
    private final BootstrapAdminUseCase bootstrap =
            new BootstrapAdminUseCase(f.users, f.hasher, f.ids, f.clock);

    @Test
    void createsAdminOnceAndIsIdempotent() {
        assertThat(bootstrap.execute("admin@devready.local", "senha-admin-1"))
                .isEqualTo(BootstrapAdminOutcome.CREATED);
        assertThat(bootstrap.execute("admin@devready.local", "outra-senha-2"))
                .isEqualTo(BootstrapAdminOutcome.ALREADY_ADMIN);

        assertThat(f.users.count()).isEqualTo(1);
        var admin = f.users.findByEmail("admin@devready.local").orElseThrow();
        assertThat(admin.role()).isEqualTo(Role.ADMIN);
        assertThat(admin.passwordHash()).isEqualTo("hashed:senha-admin-1");
    }

    @Test
    void doesNothingWithoutCredentials() {
        assertThat(bootstrap.execute(null, null)).isEqualTo(BootstrapAdminOutcome.SKIPPED);
        assertThat(bootstrap.execute("", "senha-admin-1")).isEqualTo(BootstrapAdminOutcome.SKIPPED);
        assertThat(f.users.count()).isZero();
    }

    @Test
    void refusesWeakPassword() {
        assertThatThrownBy(() -> bootstrap.execute("admin@devready.local", "troque"))
                .isInstanceOf(WeakPasswordException.class);
        assertThat(f.users.count()).isZero();
    }

    @Test
    void doesNotPromoteACandidateThatAlreadyUsesTheEmail() {
        f.users.save(
                User.register(f.ids.next(), "admin@devready.local", "Ana", "hash", f.clock.now()));

        assertThat(bootstrap.execute("admin@devready.local", "senha-admin-1"))
                .isEqualTo(BootstrapAdminOutcome.EMAIL_BELONGS_TO_CANDIDATE);
        assertThat(f.users.findByEmail("admin@devready.local").orElseThrow().role())
                .isEqualTo(Role.CANDIDATE);
    }
}
