package br.com.devready.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.ofEpochSecond(946_684_800L);

    @Test
    void registersCandidateWithNormalizedEmail() {
        User user = User.register(ID, "  Ana@Example.COM ", "Ana", "hash", NOW);

        assertThat(user.id()).isEqualTo(ID);
        assertThat(user.email()).isEqualTo("ana@example.com");
        assertThat(user.displayName()).isEqualTo("Ana");
        assertThat(user.role()).isEqualTo(Role.CANDIDATE);
        assertThat(user.active()).isTrue();
        assertThat(user.hasPassword()).isTrue();
        assertThat(user.isAdmin()).isFalse();
        assertThat(user.createdAt()).isEqualTo(NOW);
        assertThat(user.socialAccounts()).isEmpty();
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> User.register(ID, "not-an-email", "Ana", "hash", NOW))
                .isInstanceOf(InvalidEmailException.class)
                .hasMessage("E-mail inválido: not-an-email");
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThatThrownBy(() -> User.register(ID, "ana@example.com", " ", "hash", NOW))
                .isInstanceOf(IdentityException.class);
    }

    @Test
    void socialUserHasNoPasswordAndOneLinkedAccount() {
        SocialAccount account = new SocialAccount(SocialProvider.GOOGLE, "google-123");
        User user =
                User.fromSocial(ID, "ana@example.com", "Ana", "https://img/ana.png", account, NOW);

        assertThat(user.hasPassword()).isFalse();
        assertThat(user.avatarUrl()).isEqualTo("https://img/ana.png");
        assertThat(user.socialAccounts()).containsExactly(account);
        assertThat(user.hasSocialAccount(SocialProvider.GOOGLE)).isTrue();
        assertThat(user.hasSocialAccount(SocialProvider.GITHUB)).isFalse();
    }

    @Test
    void linksSecondProviderButNotTheSameProviderTwice() {
        User user = User.register(ID, "ana@example.com", "Ana", "hash", NOW);
        user.linkSocial(new SocialAccount(SocialProvider.GITHUB, "gh-1"));

        assertThatThrownBy(() -> user.linkSocial(new SocialAccount(SocialProvider.GITHUB, "gh-2")))
                .isInstanceOf(IdentityException.class)
                .hasMessage("Já existe uma conta GitHub vinculada.");
        assertThat(user.socialAccounts()).hasSize(1);
    }

    @Test
    void changesPasswordProfileRoleAndStatus() {
        User user = User.register(ID, "ana@example.com", "Ana", "hash", NOW);

        user.changePassword("hash2");
        user.updateProfile("Ana Souza", "https://img/new.png");
        user.promoteToAdmin();
        user.deactivate();

        assertThat(user.passwordHash()).isEqualTo("hash2");
        assertThat(user.displayName()).isEqualTo("Ana Souza");
        assertThat(user.avatarUrl()).isEqualTo("https://img/new.png");
        assertThat(user.isAdmin()).isTrue();
        assertThat(user.active()).isFalse();

        user.demoteToCandidate();
        user.activate();
        assertThat(user.role()).isEqualTo(Role.CANDIDATE);
        assertThat(user.active()).isTrue();
    }

    @Test
    void restoreKeepsEveryField() {
        SocialAccount account = new SocialAccount(SocialProvider.GITHUB, "gh-1");
        User user =
                User.restore(
                        ID,
                        "ana@example.com",
                        "Ana",
                        null,
                        null,
                        Role.ADMIN,
                        false,
                        true,
                        NOW,
                        List.of(account));

        assertThat(user.role()).isEqualTo(Role.ADMIN);
        assertThat(user.active()).isFalse();
        assertThat(user.mustChangePassword()).isTrue();
        assertThat(user.hasPassword()).isFalse();
        assertThat(user.socialAccounts()).containsExactly(account);
    }

    @Test
    void newAccountsDoNotHaveToChangeThePassword() {
        User registered = User.register(ID, "ana@example.com", "Ana", "hash", NOW);
        User social =
                User.fromSocial(
                        ID,
                        "ana@example.com",
                        "Ana",
                        null,
                        new SocialAccount(SocialProvider.GOOGLE, "g-1"),
                        NOW);

        assertThat(registered.mustChangePassword()).isFalse();
        assertThat(social.mustChangePassword()).isFalse();
    }

    @Test
    void aResetStoresTheTemporaryHashAndAsksForANewPassword() {
        User user = User.register(ID, "ana@example.com", "Ana", "hash", NOW);

        user.resetPassword("hash-provisorio");

        assertThat(user.passwordHash()).isEqualTo("hash-provisorio");
        assertThat(user.mustChangePassword()).isTrue();
    }

    @Test
    void aResetGivesASocialOnlyAccountItsFirstPassword() {
        User user =
                User.fromSocial(
                        ID,
                        "ana@example.com",
                        "Ana",
                        null,
                        new SocialAccount(SocialProvider.GITHUB, "gh-1"),
                        NOW);

        user.resetPassword("hash-provisorio");

        assertThat(user.hasPassword()).isTrue();
        assertThat(user.mustChangePassword()).isTrue();
    }

    @Test
    void changingThePasswordClearsTheMark() {
        User user = User.register(ID, "ana@example.com", "Ana", "hash", NOW);
        user.resetPassword("hash-provisorio");

        user.changePassword("hash-novo");

        assertThat(user.passwordHash()).isEqualTo("hash-novo");
        assertThat(user.mustChangePassword()).isFalse();
    }

    @Test
    void aResetNeedsAHash() {
        User user = User.register(ID, "ana@example.com", "Ana", "hash", NOW);

        assertThatThrownBy(() -> user.resetPassword(" ")).isInstanceOf(IdentityException.class);
        assertThat(user.passwordHash()).isEqualTo("hash");
        assertThat(user.mustChangePassword()).isFalse();
    }

    @Test
    void onlyAnActiveAdminCountsAsActiveAdmin() {
        User user = User.register(ID, "ana@example.com", "Ana", "hash", NOW);
        assertThat(user.isActiveAdmin()).isFalse();

        user.promoteToAdmin();
        assertThat(user.isActiveAdmin()).isTrue();

        user.deactivate();
        assertThat(user.isActiveAdmin()).isFalse();

        user.activate();
        user.demoteToCandidate();
        assertThat(user.isActiveAdmin()).isFalse();
    }

    @Test
    void onlyAnActiveAdminWithoutATemporaryPasswordManagesUsers() {
        User user = User.register(ID, "ana@example.com", "Ana", "hash", NOW);
        assertThat(user.canManageUsers()).isFalse();

        user.promoteToAdmin();
        assertThat(user.canManageUsers()).isTrue();

        user.resetPassword("hash-provisorio");
        assertThat(user.isActiveAdmin()).isTrue();
        assertThat(user.canManageUsers()).isFalse();

        user.changePassword("hash-novo");
        assertThat(user.canManageUsers()).isTrue();

        user.deactivate();
        assertThat(user.canManageUsers()).isFalse();
    }
}
