package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.dto.OneTimeCode;
import br.com.devready.application.identity.dto.SocialLoginCommand;
import br.com.devready.application.identity.exception.UserInactiveException;
import br.com.devready.application.identity.port.OneTimeCodeStore;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.application.shared.exception.ApplicationException;
import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.User;
import java.util.Optional;

public class LoginWithSocialAccountUseCase {

    private final UserRepository users;
    private final OneTimeCodeStore oneTimeCodes;
    private final IdGenerator ids;
    private final Clock clock;

    public LoginWithSocialAccountUseCase(
            UserRepository users, OneTimeCodeStore oneTimeCodes, IdGenerator ids, Clock clock) {
        this.users = users;
        this.oneTimeCodes = oneTimeCodes;
        this.ids = ids;
        this.clock = clock;
    }

    public OneTimeCode execute(SocialLoginCommand command) {
        SocialAccount account = new SocialAccount(command.provider(), command.providerUserId());
        User user =
                users.findBySocialAccount(account).orElseGet(() -> linkOrCreate(command, account));

        requireActive(user);
        return new OneTimeCode(oneTimeCodes.issue(user.id()));
    }

    private User linkOrCreate(SocialLoginCommand command, SocialAccount account) {
        if (command.email() == null || command.email().isBlank()) {
            throw new ApplicationException(
                    "O provedor não informou um e-mail. Não dá para criar a conta.");
        }
        String email = User.normalizeEmail(command.email());
        String displayName = displayNameFor(command, email);
        return linkExisting(email, account)
                .orElseGet(
                        () ->
                                users.save(
                                        User.fromSocial(
                                                ids.next(),
                                                email,
                                                displayName,
                                                command.avatarUrl(),
                                                account,
                                                clock.now())));
    }

    private Optional<User> linkExisting(String email, SocialAccount account) {
        return users.findByEmail(email)
                .map(
                        existing -> {
                            requireActive(existing);
                            existing.linkSocial(account);
                            users.linkSocialAccount(existing.id(), account);
                            return existing;
                        });
    }

    private static void requireActive(User user) {
        if (!user.active()) {
            throw new UserInactiveException();
        }
    }

    private static String displayNameFor(SocialLoginCommand command, String email) {
        if (command.displayName() != null && !command.displayName().isBlank()) {
            return command.displayName();
        }
        return email.substring(0, email.indexOf('@'));
    }
}
