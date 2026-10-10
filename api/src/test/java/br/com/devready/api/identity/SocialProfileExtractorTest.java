package br.com.devready.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.identity.SocialProvider;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SocialProfileExtractorTest {

    @Test
    void mapsGoogleAttributes() {
        var command =
                SocialProfileExtractor.extract(
                        "google",
                        Map.of(
                                "sub",
                                "g-1",
                                "email",
                                "ana@example.com",
                                "email_verified",
                                true,
                                "name",
                                "Ana",
                                "picture",
                                "https://img/a.png"),
                        Optional::empty);

        assertThat(command.provider()).isEqualTo(SocialProvider.GOOGLE);
        assertThat(command.providerUserId()).isEqualTo("g-1");
        assertThat(command.email()).isEqualTo("ana@example.com");
        assertThat(command.displayName()).isEqualTo("Ana");
        assertThat(command.avatarUrl()).isEqualTo("https://img/a.png");
    }

    @Test
    void dropsGoogleEmailThatIsNotVerified() {
        var unverified =
                SocialProfileExtractor.extract(
                        "google",
                        Map.of("sub", "g-2", "email", "ana@example.com", "email_verified", false),
                        Optional::empty);
        var missingFlag =
                SocialProfileExtractor.extract(
                        "google",
                        Map.of("sub", "g-3", "email", "ana@example.com"),
                        Optional::empty);

        assertThat(unverified.email()).isNull();
        assertThat(missingFlag.email()).isNull();
        assertThat(unverified.providerUserId()).isEqualTo("g-2");
    }

    @Test
    void mapsGitHubAttributesUsingLoginWhenNameIsMissingAndFallbackWhenEmailIsNull() {
        Map<String, Object> attributes = new java.util.HashMap<>();
        attributes.put("id", 42);
        attributes.put("login", "anadev");
        attributes.put("name", null);
        attributes.put("email", null);
        attributes.put("avatar_url", "https://img/gh.png");

        var command =
                SocialProfileExtractor.extract(
                        "github", attributes, () -> Optional.of("ana@example.com"));

        assertThat(command.provider()).isEqualTo(SocialProvider.GITHUB);
        assertThat(command.providerUserId()).isEqualTo("42");
        assertThat(command.email()).isEqualTo("ana@example.com");
        assertThat(command.displayName()).isEqualTo("anadev");
        assertThat(command.avatarUrl()).isEqualTo("https://img/gh.png");
    }

    @Test
    void rejectsUnknownProvider() {
        assertThatThrownBy(
                        () -> SocialProfileExtractor.extract("facebook", Map.of(), Optional::empty))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
