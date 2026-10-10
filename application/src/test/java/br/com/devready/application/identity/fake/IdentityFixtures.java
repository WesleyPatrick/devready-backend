package br.com.devready.application.identity.fake;

import br.com.devready.application.identity.service.IssueSession;
import br.com.devready.application.shared.DirectTransactions;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import java.time.Instant;

public class IdentityFixtures {
    public final InMemoryUserRepository users = new InMemoryUserRepository();
    public final FakePasswordHasher hasher = new FakePasswordHasher();
    public final FakeTokenIssuer tokenIssuer = new FakeTokenIssuer();
    public final InMemoryRefreshTokenStore refreshTokens = new InMemoryRefreshTokenStore();
    public final InMemoryOneTimeCodeStore oneTimeCodes = new InMemoryOneTimeCodeStore();
    public final FixedClock clock = new FixedClock(Instant.ofEpochSecond(946_684_800L));
    public final SequentialIdGenerator ids = new SequentialIdGenerator();
    public final DirectTransactions transactions = new DirectTransactions();
    public final FakePasswordGenerator passwords = new FakePasswordGenerator();
    public final IssueSession issueSession = new IssueSession(tokenIssuer, refreshTokens);
}
