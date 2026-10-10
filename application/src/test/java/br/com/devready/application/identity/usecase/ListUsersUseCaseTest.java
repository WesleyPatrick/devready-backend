package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.UserPage;
import br.com.devready.application.identity.dto.UserQuery;
import br.com.devready.application.identity.exception.NotAnActiveAdminException;
import br.com.devready.application.identity.fake.InMemoryUserRepository;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListUsersUseCaseTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final ListUsersUseCase list = new ListUsersUseCase(users);

    private final User actor = actor();

    private User actor() {
        User admin =
                User.register(
                        new UUID(7, 7), "admin@example.com", "Admin", "hash", T0.minusSeconds(1));
        admin.promoteToAdmin();
        return users.save(admin);
    }

    private User person(int n, String name, String email) {
        return users.save(User.register(new UUID(0, n), email, name, "hash", T0.plusSeconds(n)));
    }

    private void people(int count) {
        for (int n = 1; n <= count; n++) {
            person(n, "Pessoa " + n, "pessoa" + n + "@example.com");
        }
    }

    private static List<String> names(UserPage page) {
        return page.items().stream().map(User::displayName).toList();
    }

    private UserPage list(UserQuery query) {
        return list.execute(actor.id(), query);
    }

    private UserPage all(int page, int size) {
        return list(new UserQuery(null, null, null, page, size));
    }

    @Test
    void newestFirstInPagesOfTheRequestedSize() {
        people(5);

        UserPage first = all(1, 2);

        assertThat(names(first)).containsExactly("Pessoa 5", "Pessoa 4");
        assertThat(first.page()).isEqualTo(1);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.totalItems()).isEqualTo(6);
        assertThat(names(all(3, 2))).containsExactly("Pessoa 1", "Admin");
    }

    @Test
    void aPageAfterTheLastIsTheLastAndBeforeTheFirstIsTheFirst() {
        people(3);

        UserPage beyond = all(9, 2);

        assertThat(beyond.page()).isEqualTo(2);
        assertThat(names(beyond)).containsExactly("Pessoa 1", "Admin");
        assertThat(all(0, 2).page()).isEqualTo(1);
        assertThat(names(all(-3, 2))).containsExactly("Pessoa 3", "Pessoa 2");
    }

    @Test
    void theSizeStaysBetweenOneAndFifty() {
        people(60);

        assertThat(all(1, 500).items()).hasSize(50);
        assertThat(all(1, 0).items()).hasSize(1);
        assertThat(all(1, 0).totalPages()).isEqualTo(61);
    }

    @Test
    void searchesNameOrEmailIgnoringCaseAndSurroundingSpaces() {
        person(1, "Elis Teste", "elis-sessao@example.com");
        person(2, "Fábio", "fabio-sessao@example.com");
        person(3, "Ana", "ana@ELISEU.com");

        assertThat(names(list(new UserQuery("  ELIS ", null, null, 1, 20))))
                .containsExactly("Ana", "Elis Teste");
        assertThat(names(list(new UserQuery("Sessao", null, null, 1, 20))))
                .containsExactly("Fábio", "Elis Teste");
        assertThat(names(list(new UserQuery("   ", null, null, 1, 20)))).hasSize(4);
    }

    @Test
    void filtersByRoleAndSituation() {
        person(1, "Ana", "ana@example.com");
        User bia = person(2, "Bia", "bia@example.com");
        bia.promoteToAdmin();
        users.save(bia);
        User caio = person(3, "Caio", "caio@example.com");
        caio.deactivate();
        users.save(caio);

        assertThat(names(list(new UserQuery(null, Role.ADMIN, null, 1, 20))))
                .containsExactly("Bia", "Admin");
        assertThat(names(list(new UserQuery(null, Role.CANDIDATE, null, 1, 20))))
                .containsExactly("Caio", "Ana");
        assertThat(names(list(new UserQuery(null, null, false, 1, 20)))).containsExactly("Caio");
        assertThat(names(list(new UserQuery(null, Role.CANDIDATE, true, 1, 20))))
                .containsExactly("Ana");
    }

    @Test
    void nothingFoundIsOnePageWithNothing() {
        people(2);

        UserPage page = list(new UserQuery("ninguém", null, null, 4, 20));

        assertThat(page.items()).isEmpty();
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.totalItems()).isZero();
    }

    @Test
    void anAdminDemotedOrDeactivatedAfterSigningInIsRefused() {
        people(2);
        actor.demoteToCandidate();
        users.save(actor);

        assertThatThrownBy(() -> all(1, 20))
                .isInstanceOf(NotAnActiveAdminException.class)
                .hasMessage("Você não tem mais acesso à gestão de usuários.");

        actor.promoteToAdmin();
        actor.deactivate();
        users.save(actor);

        assertThatThrownBy(() -> all(1, 20)).isInstanceOf(NotAnActiveAdminException.class);
    }

    @Test
    void anActorThatDoesNotExistIsRefused() {
        people(2);

        assertThatThrownBy(
                        () -> list.execute(new UUID(8, 8), new UserQuery(null, null, null, 1, 20)))
                .isInstanceOf(NotAnActiveAdminException.class);
    }

    @Test
    void anAdminWithAPendingTemporaryPasswordIsRefused() {
        people(2);
        actor.resetPassword("hash-provisorio");
        users.save(actor);

        assertThatThrownBy(() -> all(1, 20))
                .isInstanceOf(NotAnActiveAdminException.class)
                .hasMessage("Você não tem mais acesso à gestão de usuários.");
    }
}
