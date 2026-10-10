package br.com.devready.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class GitHubApiEmailResolverTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer github = MockRestServiceServer.bindTo(builder).build();
    private final GitHubApiEmailResolver resolver = new GitHubApiEmailResolver(builder);

    private void respond(String json) {
        github.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(header("Authorization", "Bearer gh-token"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void returnsThePrimaryVerifiedEmail() {
        respond(
                """
                [{"email":"velho@example.com","primary":false,"verified":true},
                 {"email":"ana@example.com","primary":true,"verified":true}]
                """);

        assertThat(resolver.primaryEmail("gh-token")).contains("ana@example.com");
    }

    @Test
    void ignoresEmailsThatAreNotVerified() {
        respond(
                """
                [{"email":"ana@example.com","primary":true,"verified":false},
                 {"email":"outro@example.com","primary":false,"verified":false}]
                """);

        assertThat(resolver.primaryEmail("gh-token")).isEmpty();
    }

    @Test
    void propagatesHttpFailureForTheHandlerToTurnIntoALoginError() {
        github.expect(requestTo("https://api.github.com/user/emails"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> resolver.primaryEmail("gh-token"))
                .isInstanceOf(RestClientException.class);
    }
}
