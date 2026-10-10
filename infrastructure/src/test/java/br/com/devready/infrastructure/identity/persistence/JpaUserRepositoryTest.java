package br.com.devready.infrastructure.identity.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.identity.dto.UserFilter;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.SocialProvider;
import br.com.devready.domain.identity.User;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestcontainersConfiguration.class, JpaUserRepository.class})
class JpaUserRepositoryTest {

    @Autowired private JpaUserRepository repository;

    private static final Instant NOW = Instant.ofEpochSecond(946_684_800L);

    @Test
    void savesAndFindsByIdEmailAndSocialAccount() {
        UUID id = UUID.randomUUID();
        User user = User.register(id, "ana@example.com", "Ana", "hash", NOW);
        user.linkSocial(new SocialAccount(SocialProvider.GITHUB, "gh-1"));

        repository.save(user);

        User byId = repository.findById(id).orElseThrow();
        assertThat(byId.email()).isEqualTo("ana@example.com");
        assertThat(byId.displayName()).isEqualTo("Ana");
        assertThat(byId.passwordHash()).isEqualTo("hash");
        assertThat(byId.role()).isEqualTo(Role.CANDIDATE);
        assertThat(byId.active()).isTrue();
        assertThat(byId.createdAt()).isEqualTo(NOW);
        assertThat(byId.socialAccounts())
                .containsExactly(new SocialAccount(SocialProvider.GITHUB, "gh-1"));

        assertThat(repository.findByEmail("ana@example.com")).isPresent();
        assertThat(repository.existsByEmail("ana@example.com")).isTrue();
        assertThat(repository.existsByEmail("ninguem@example.com")).isFalse();
        assertThat(repository.findBySocialAccount(new SocialAccount(SocialProvider.GITHUB, "gh-1")))
                .map(User::id)
                .contains(id);
        assertThat(repository.findBySocialAccount(new SocialAccount(SocialProvider.GOOGLE, "gh-1")))
                .isEmpty();
    }

    @Test
    void updatesExistingUserInPlace() {
        UUID id = UUID.randomUUID();
        repository.save(User.register(id, "bob@example.com", "Bob", "hash", NOW));

        User bob = repository.findById(id).orElseThrow();
        bob.updateProfile("Roberto", "https://img/b.png");
        bob.promoteToAdmin();
        bob.deactivate();
        bob.linkSocial(new SocialAccount(SocialProvider.GOOGLE, "g-1"));
        repository.save(bob);

        User reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.displayName()).isEqualTo("Roberto");
        assertThat(reloaded.avatarUrl()).isEqualTo("https://img/b.png");
        assertThat(reloaded.isAdmin()).isTrue();
        assertThat(reloaded.active()).isFalse();
        assertThat(reloaded.socialAccounts()).hasSize(1);
    }

    @Test
    void savingTheProfileKeepsTheAccessAndThePasswordWrittenMeanwhile() {
        UUID id = UUID.randomUUID();
        repository.save(User.register(id, "caio@example.com", "Caio", "hash", NOW));
        User readByTheProfile = repository.findById(id).orElseThrow();
        User changedByAnAdmin = repository.findById(id).orElseThrow();
        changedByAnAdmin.deactivate();
        changedByAnAdmin.promoteToAdmin();
        changedByAnAdmin.resetPassword("hash-provisorio");
        repository.save(changedByAnAdmin);

        readByTheProfile.updateProfile("Caio Lima", "https://img/c.png");
        repository.saveProfile(readByTheProfile);

        User reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.displayName()).isEqualTo("Caio Lima");
        assertThat(reloaded.avatarUrl()).isEqualTo("https://img/c.png");
        assertThat(reloaded.passwordHash()).isEqualTo("hash-provisorio");
        assertThat(reloaded.mustChangePassword()).isTrue();
        assertThat(reloaded.active()).isFalse();
        assertThat(reloaded.isAdmin()).isTrue();
    }

    @Test
    void theMarkOfATemporaryPasswordIsStoredAndRead() {
        UUID id = UUID.randomUUID();
        User user = User.register(id, "edu@example.com", "Edu", "hash", NOW);
        repository.save(user);
        assertThat(repository.findById(id).orElseThrow().mustChangePassword()).isFalse();

        user.resetPassword("hash-provisorio");
        repository.save(user);

        assertThat(repository.findById(id).orElseThrow().mustChangePassword()).isTrue();
    }

    @Test
    void savingTheCredentialsKeepsEverythingElseWrittenMeanwhile() {
        UUID id = UUID.randomUUID();
        User original = User.register(id, "fabi@example.com", "Fabi", "hash", NOW);
        original.promoteToAdmin();
        repository.save(original);
        User readByTheReset = repository.findById(id).orElseThrow();
        User changedMeanwhile = repository.findById(id).orElseThrow();
        changedMeanwhile.demoteToCandidate();
        changedMeanwhile.deactivate();
        repository.saveAccess(changedMeanwhile);
        changedMeanwhile.updateProfile("Fabi Rocha", "https://img/f.png");
        repository.saveProfile(changedMeanwhile);

        readByTheReset.resetPassword("hash-provisorio");
        repository.saveCredentials(readByTheReset);

        User reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.passwordHash()).isEqualTo("hash-provisorio");
        assertThat(reloaded.mustChangePassword()).isTrue();
        assertThat(reloaded.role()).isEqualTo(Role.CANDIDATE);
        assertThat(reloaded.active()).isFalse();
        assertThat(reloaded.displayName()).isEqualTo("Fabi Rocha");
        assertThat(reloaded.avatarUrl()).isEqualTo("https://img/f.png");

        reloaded.changePassword("hash-novo");
        repository.saveCredentials(reloaded);

        User afterTheChange = repository.findById(id).orElseThrow();
        assertThat(afterTheChange.passwordHash()).isEqualTo("hash-novo");
        assertThat(afterTheChange.mustChangePassword()).isFalse();
    }

    @Test
    void savingTheAccessKeepsTheProfileWrittenMeanwhile() {
        UUID id = UUID.randomUUID();
        repository.save(User.register(id, "dora@example.com", "Dora", "hash", NOW));
        User readByTheAdmin = repository.findById(id).orElseThrow();
        User changedByTheProfile = repository.findById(id).orElseThrow();
        changedByTheProfile.updateProfile("Dora Reis", "https://img/d.png");
        changedByTheProfile.changePassword("hash-novo");
        repository.saveCredentials(changedByTheProfile);
        repository.saveProfile(changedByTheProfile);

        readByTheAdmin.deactivate();
        readByTheAdmin.promoteToAdmin();
        repository.saveAccess(readByTheAdmin);

        User reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.active()).isFalse();
        assertThat(reloaded.isAdmin()).isTrue();
        assertThat(reloaded.displayName()).isEqualTo("Dora Reis");
        assertThat(reloaded.avatarUrl()).isEqualTo("https://img/d.png");
        assertThat(reloaded.passwordHash()).isEqualTo("hash-novo");
    }

    @Test
    void replacingThePasswordOnlyWritesIfTheHashIsStillTheOneThatWasChecked() {
        UUID id = UUID.randomUUID();
        repository.save(User.register(id, "gil@example.com", "Gil", "hash-atual", NOW));
        User changedByAnAdmin = repository.findById(id).orElseThrow();
        changedByAnAdmin.resetPassword("hash-provisorio");
        repository.saveCredentials(changedByAnAdmin);

        assertThat(repository.replacePassword(id, "hash-atual", "hash-novo")).isFalse();

        User afterTheRace = repository.findById(id).orElseThrow();
        assertThat(afterTheRace.passwordHash()).isEqualTo("hash-provisorio");
        assertThat(afterTheRace.mustChangePassword()).isTrue();

        assertThat(repository.replacePassword(id, "hash-provisorio", "hash-novo")).isTrue();

        User changed = repository.findById(id).orElseThrow();
        assertThat(changed.passwordHash()).isEqualTo("hash-novo");
        assertThat(changed.mustChangePassword()).isFalse();
    }

    @Test
    void replacingThePasswordOfAnAccountWithoutOneExpectsNoHash() {
        UUID id = UUID.randomUUID();
        repository.save(
                User.fromSocial(
                        id,
                        "hel@example.com",
                        "Hel",
                        null,
                        new SocialAccount(SocialProvider.GOOGLE, "g-" + id),
                        NOW));

        assertThat(repository.replacePassword(id, "qualquer", "hash-novo")).isFalse();
        assertThat(repository.findById(id).orElseThrow().hasPassword()).isFalse();

        assertThat(repository.replacePassword(id, null, "hash-novo")).isTrue();
        assertThat(repository.findById(id).orElseThrow().passwordHash()).isEqualTo("hash-novo");

        assertThat(repository.replacePassword(id, null, "outro-hash")).isFalse();
        assertThat(repository.findById(id).orElseThrow().passwordHash()).isEqualTo("hash-novo");
    }

    @Test
    void linkingASocialAccountKeepsEverythingWrittenMeanwhile() {
        UUID id = UUID.randomUUID();
        repository.save(User.register(id, "ivo@example.com", "Ivo", "hash", NOW));
        User readByTheLogin = repository.findById(id).orElseThrow();
        User changedByAnAdmin = repository.findById(id).orElseThrow();
        changedByAnAdmin.resetPassword("hash-provisorio");
        changedByAnAdmin.promoteToAdmin();
        repository.save(changedByAnAdmin);

        SocialAccount github = new SocialAccount(SocialProvider.GITHUB, "gh-" + id);
        readByTheLogin.linkSocial(github);
        repository.linkSocialAccount(id, github);

        User reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.socialAccounts()).containsExactly(github);
        assertThat(reloaded.passwordHash()).isEqualTo("hash-provisorio");
        assertThat(reloaded.mustChangePassword()).isTrue();
        assertThat(reloaded.isAdmin()).isTrue();
        assertThat(repository.findBySocialAccount(github)).map(User::id).contains(id);
    }

    private static String token() {
        return "zz" + UUID.randomUUID().toString().substring(0, 8);
    }

    private User saved(String email, String name, int secondsAfter) {
        return repository.save(
                User.register(
                        UUID.randomUUID(), email, name, "hash", NOW.plusSeconds(secondsAfter)));
    }

    @Test
    void searchesNameOrEmailIgnoringCaseNewestFirstInPages() {
        String token = token();
        User first = saved(token + "-ana@example.com", "Ana Lima", 0);
        User second = saved("bia-" + UUID.randomUUID() + "@example.com", "Bia " + token, 60);
        User third = saved(token + "-caio@example.com", "Caio Souza", 120);
        saved("outra-" + UUID.randomUUID() + "@example.com", "Outra Pessoa", 180);
        UserFilter filter = new UserFilter(token.toUpperCase(Locale.ROOT), null, null);

        assertThat(repository.count(filter)).isEqualTo(3);
        assertThat(repository.find(filter, 1, 2))
                .extracting(User::id)
                .containsExactly(third.id(), second.id());
        assertThat(repository.find(filter, 2, 2)).extracting(User::id).containsExactly(first.id());
    }

    @Test
    void filtersByRoleAndSituationTogetherWithTheSearch() {
        String token = token();
        User ana = saved(token + "-ana@example.com", "Ana", 0);
        User bia = saved(token + "-bia@example.com", "Bia", 60);
        bia.promoteToAdmin();
        repository.save(bia);
        User caio = saved(token + "-caio@example.com", "Caio", 120);
        caio.deactivate();
        repository.save(caio);

        assertThat(repository.find(new UserFilter(token, Role.ADMIN, null), 1, 20))
                .extracting(User::id)
                .containsExactly(bia.id());
        assertThat(repository.find(new UserFilter(token, null, false), 1, 20))
                .extracting(User::id)
                .containsExactly(caio.id());
        assertThat(repository.find(new UserFilter(token, Role.CANDIDATE, true), 1, 20))
                .extracting(User::id)
                .containsExactly(ana.id());
        assertThat(repository.count(new UserFilter(token, Role.CANDIDATE, null))).isEqualTo(2);
    }

    @Test
    void percentAndUnderscoreInTheSearchAreJustText() {
        String token = token();
        User literal = saved(token + "-a@example.com", "Desconto 100%_" + token, 0);
        saved(token + "-b@example.com", "Desconto 1000x" + token, 60);

        assertThat(repository.find(new UserFilter("100%_" + token, null, null), 1, 20))
                .extracting(User::id)
                .containsExactly(literal.id());
    }

    @Test
    void countsOnlyTheActiveAdmins() {
        int before = repository.lockActiveAdmins();
        User admin = saved("adm-" + UUID.randomUUID() + "@example.com", "Adm", 0);
        admin.promoteToAdmin();
        repository.save(admin);
        User inactive = saved("adm-" + UUID.randomUUID() + "@example.com", "Adm inativo", 0);
        inactive.promoteToAdmin();
        inactive.deactivate();
        repository.save(inactive);
        saved("cand-" + UUID.randomUUID() + "@example.com", "Candidato", 0);

        assertThat(repository.lockActiveAdmins()).isEqualTo(before + 1);
    }

    @Test
    void searchIgnoresTheCaseOfTheStoredName() {
        String token = token();
        User byName =
                saved(
                        "n-" + UUID.randomUUID() + "@example.com",
                        "Nome " + token.toUpperCase(Locale.ROOT),
                        0);

        assertThat(repository.find(new UserFilter(token, null, null), 1, 20))
                .extracting(User::id)
                .containsExactly(byName.id());
    }
}
