package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.AuthTokens;
import br.com.devready.application.identity.dto.RegisterUserCommand;
import br.com.devready.application.identity.exception.EmailAlreadyRegisteredException;
import br.com.devready.application.identity.fake.IdentityFixtures;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.WeakPasswordException;
import org.junit.jupiter.api.Test;

class RegisterUserUseCaseTest {

    private final IdentityFixtures f = new IdentityFixtures();
    private final RegisterUserUseCase registerUser =
            new RegisterUserUseCase(f.users, f.hasher, f.issueSession, f.ids, f.clock);

    @Test
    void createsCandidateStoresHashAndStartsSession() {
        AuthTokens tokens =
                registerUser.execute(
                        new RegisterUserCommand("Ana@Example.com", "senha-forte", "Ana"));

        assertThat(f.users.count()).isEqualTo(1);
        assertThat(tokens.user().email()).isEqualTo("ana@example.com");
        assertThat(tokens.user().role()).isEqualTo(Role.CANDIDATE);
        assertThat(tokens.user().passwordHash()).isEqualTo("hashed:senha-forte");
        assertThat(tokens.accessToken()).isEqualTo("access-for-" + tokens.user().id());
        assertThat(f.refreshTokens.isActive(tokens.refreshToken())).isTrue();
        assertThat(tokens.expiresInSeconds()).isEqualTo(900);
        assertThat(tokens.user().createdAt()).isEqualTo(f.clock.now());
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        registerUser.execute(new RegisterUserCommand("ana@example.com", "senha-forte", "Ana"));

        assertThatThrownBy(
                        () ->
                                registerUser.execute(
                                        new RegisterUserCommand(
                                                "ANA@example.com", "outra-senha", "Ana")))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessage("Já existe uma conta com o e-mail ana@example.com.");
        assertThat(f.users.count()).isEqualTo(1);
    }

    @Test
    void rejectsWeakPasswordBeforeTouchingTheRepository() {
        assertThatThrownBy(
                        () ->
                                registerUser.execute(
                                        new RegisterUserCommand("ana@example.com", "curta", "Ana")))
                .isInstanceOf(WeakPasswordException.class);
        assertThat(f.users.count()).isZero();
    }
}
