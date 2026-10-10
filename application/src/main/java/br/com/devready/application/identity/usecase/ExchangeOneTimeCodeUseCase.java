package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.dto.AuthTokens;
import br.com.devready.application.identity.exception.InvalidOneTimeCodeException;
import br.com.devready.application.identity.port.OneTimeCodeStore;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.identity.service.IssueSession;
import br.com.devready.domain.identity.User;

public class ExchangeOneTimeCodeUseCase {

    private final UserRepository users;
    private final OneTimeCodeStore oneTimeCodes;
    private final IssueSession issueSession;

    public ExchangeOneTimeCodeUseCase(
            UserRepository users, OneTimeCodeStore oneTimeCodes, IssueSession issueSession) {
        this.users = users;
        this.oneTimeCodes = oneTimeCodes;
        this.issueSession = issueSession;
    }

    public AuthTokens execute(String code) {
        User user =
                oneTimeCodes
                        .consume(code)
                        .flatMap(users::findById)
                        .orElseThrow(InvalidOneTimeCodeException::new);
        return issueSession.forUser(user);
    }
}
