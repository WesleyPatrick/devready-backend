package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.AuthTokens;
import br.com.devready.application.identity.exception.InvalidOneTimeCodeException;
import br.com.devready.application.identity.fake.IdentityFixtures;
import br.com.devready.domain.identity.User;
import org.junit.jupiter.api.Test;

class ExchangeOneTimeCodeUseCaseTest {

    private final IdentityFixtures f = new IdentityFixtures();
    private final ExchangeOneTimeCodeUseCase exchange =
            new ExchangeOneTimeCodeUseCase(f.users, f.oneTimeCodes, f.issueSession);

    @Test
    void exchangesCodeOnceForTokens() {
        User ana =
                f.users.save(
                        User.register(
                                f.ids.next(), "ana@example.com", "Ana", "hash", f.clock.now()));
        String code = f.oneTimeCodes.issue(ana.id());

        AuthTokens tokens = exchange.execute(code);

        assertThat(tokens.user().id()).isEqualTo(ana.id());
        assertThatThrownBy(() -> exchange.execute(code))
                .isInstanceOf(InvalidOneTimeCodeException.class);
    }

    @Test
    void rejectsUnknownCode() {
        assertThatThrownBy(() -> exchange.execute("nope"))
                .isInstanceOf(InvalidOneTimeCodeException.class);
    }
}
