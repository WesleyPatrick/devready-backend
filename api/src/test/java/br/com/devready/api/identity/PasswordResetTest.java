package br.com.devready.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.application.identity.dto.SocialLoginCommand;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.identity.usecase.LoginWithSocialAccountUseCase;
import br.com.devready.domain.identity.SocialProvider;
import br.com.devready.domain.identity.User;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = "devready.seed.enabled=true")
class PasswordResetTest extends IntegrationTest {

    private static final String BLOCKED_DETAIL = "Defina uma senha nova para continuar.";

    @Autowired private UserRepository users;
    @Autowired private LoginWithSocialAccountUseCase loginWithSocialAccount;

    private record Session(UUID id, String access, String refresh) {
        String bearer() {
            return "Bearer " + access;
        }
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private static String body(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static Session sessionOf(String body) {
        return new Session(
                UUID.fromString(JsonPath.read(body, "$.user.id")),
                JsonPath.read(body, "$.accessToken"),
                JsonPath.read(body, "$.refreshToken"));
    }

    private Session register(String email) throws Exception {
        return sessionOf(
                body(
                        mockMvc.perform(
                                        post("/api/v1/auth/register")
                                                .contentType(APPLICATION_JSON)
                                                .content(
                                                        "{\"email\":\""
                                                                + email
                                                                + "\",\"password\":\"senha-forte\","
                                                                + "\"displayName\":\"Pessoa\"}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.user.mustChangePassword").value(false))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(
                                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private Session newAdmin() throws Exception {
        String email = uniqueEmail("admin");
        UUID id = register(email).id();
        User user = users.findById(id).orElseThrow();
        user.promoteToAdmin();
        users.save(user);
        return sessionOf(body(login(email, "senha-forte").andExpect(status().isOk())));
    }

    private ResultActions reset(Session admin, UUID target) throws Exception {
        return mockMvc.perform(
                post("/api/v1/admin/users/" + target + "/password-reset")
                        .header("Authorization", admin.bearer()));
    }

    private String temporaryPasswordFor(Session admin, UUID target) throws Exception {
        return JsonPath.read(
                body(reset(admin, target).andExpect(status().isOk())), "$.temporaryPassword");
    }

    private void goal(Session session) throws Exception {
        mockMvc.perform(
                        put("/api/v1/career/profile")
                                .header("Authorization", session.bearer())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"companies\":[\"nubank\"],\"stack\":\"java-spring\","
                                                + "\"seniority\":\"JUNIOR\"}"))
                .andExpect(status().isOk());
    }

    private ResultActions questions(Session session) throws Exception {
        return mockMvc.perform(get("/api/v1/questions").header("Authorization", session.bearer()));
    }

    private ResultActions me(Session session) throws Exception {
        return mockMvc.perform(get("/api/v1/auth/me").header("Authorization", session.bearer()));
    }

    private ResultActions patchMe(Session session, String newPassword, String currentPassword)
            throws Exception {
        return mockMvc.perform(
                patch("/api/v1/me")
                        .header("Authorization", session.bearer())
                        .contentType(APPLICATION_JSON)
                        .content(
                                "{\"displayName\":\"Pessoa\",\"newPassword\":\""
                                        + newPassword
                                        + "\",\"currentPassword\":\""
                                        + currentPassword
                                        + "\"}"));
    }

    private static void assertBlocked(ResultActions actions) throws Exception {
        actions.andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Senha provisória"))
                .andExpect(jsonPath("$.detail").value(BLOCKED_DETAIL))
                .andExpect(jsonPath("$.code").value("password_change_required"));
    }

    private static String claimsOf(String accessToken) {
        String payload = accessToken.split("\\.")[1];
        return new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
    }

    @Test
    void theTemporaryPasswordOnlyOpensTheAccountUntilANewOneIsChosen() throws Exception {
        Session admin = newAdmin();
        String email = uniqueEmail("suporte");
        Session before = register(email);
        goal(before);

        String temporary = temporaryPasswordFor(admin, before.id());

        assertThat(temporary).hasSize(16).matches("[A-Za-z0-9]{16}");
        refresh(before.refresh()).andExpect(status().isUnauthorized());
        login(email, "senha-forte").andExpect(status().isUnauthorized());

        Session marked =
                sessionOf(
                        body(
                                login(email, temporary)
                                        .andExpect(status().isOk())
                                        .andExpect(
                                                jsonPath("$.user.mustChangePassword")
                                                        .value(true))));
        assertThat(JsonPath.<Boolean>read(claimsOf(marked.access()), "$.pwd_change")).isTrue();
        assertBlocked(questions(marked));
        me(marked)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.hasPassword").value(true));

        Session renewed =
                sessionOf(
                        body(
                                refresh(marked.refresh())
                                        .andExpect(status().isOk())
                                        .andExpect(
                                                jsonPath("$.user.mustChangePassword")
                                                        .value(true))));
        assertBlocked(questions(renewed));

        patchMe(renewed, temporary, temporary)
                .andExpect(status().isUnprocessableContent())
                .andExpect(
                        jsonPath("$.detail")
                                .value("A senha nova precisa ser diferente da provisória."));
        patchMe(renewed, "minha-senha-nova", temporary)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
        refresh(renewed.refresh()).andExpect(status().isUnauthorized());
        login(email, temporary).andExpect(status().isUnauthorized());

        Session after =
                sessionOf(
                        body(
                                login(email, "minha-senha-nova")
                                        .andExpect(status().isOk())
                                        .andExpect(
                                                jsonPath("$.user.mustChangePassword")
                                                        .value(false))));
        assertThat(claimsOf(after.access())).doesNotContain("pwd_change");
        questions(after).andExpect(status().isOk());
        assertThat(users.findById(before.id()).orElseThrow().mustChangePassword()).isFalse();
    }

    @Test
    void theAnswerIsNotCachedAndTheMarkDoesNotTouchTheAdmin() throws Exception {
        Session admin = newAdmin();
        Session target = register(uniqueEmail("cache"));

        reset(admin, target.id())
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.temporaryPassword").isString());

        me(admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", admin.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void withTheMarkOnlyTheAccountAndSessionRoutesAnswer() throws Exception {
        Session admin = newAdmin();
        String email = uniqueEmail("rotas");
        Session target = register(email);
        String temporary = temporaryPasswordFor(admin, target.id());
        Session marked = sessionOf(body(login(email, temporary).andExpect(status().isOk())));
        Session other = sessionOf(body(login(email, temporary).andExpect(status().isOk())));

        assertBlocked(
                mockMvc.perform(
                        put("/api/v1/career/profile")
                                .header("Authorization", marked.bearer())
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"companies\":[\"nubank\"],\"stack\":\"java-spring\","
                                                + "\"seniority\":\"JUNIOR\"}")));
        assertBlocked(
                mockMvc.perform(
                        get("/api/v1/me/progress").header("Authorization", marked.bearer())));
        assertBlocked(
                mockMvc.perform(get("/api/v1/auth/me/").header("Authorization", marked.bearer())));
        assertBlocked(
                mockMvc.perform(get("/api/v1/nada-aqui").header("Authorization", marked.bearer())));
        assertBlocked(
                mockMvc.perform(
                        get("/api/v1/admin/users").header("Authorization", marked.bearer())));

        me(marked).andExpect(status().isOk());
        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .header("Authorization", marked.bearer())
                                .contentType(APPLICATION_JSON)
                                .content("{\"refreshToken\":\"" + marked.refresh() + "\"}"))
                .andExpect(status().isNoContent());
        refresh(marked.refresh()).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/logout-all").header("Authorization", marked.bearer()))
                .andExpect(status().isNoContent());
        refresh(other.refresh()).andExpect(status().isUnauthorized());
    }

    @Test
    void anAdminWithTheMarkAlsoLosesTheManagementUntilTheChange() throws Exception {
        Session ana = newAdmin();
        Session bruno = newAdmin();
        String brunoEmail = JsonPath.read(body(me(bruno).andExpect(status().isOk())), "$.email");

        String temporary = temporaryPasswordFor(ana, bruno.id());
        Session marked = sessionOf(body(login(brunoEmail, temporary).andExpect(status().isOk())));

        assertBlocked(
                mockMvc.perform(
                        get("/api/v1/admin/users").header("Authorization", marked.bearer())));
        assertBlocked(reset(marked, ana.id()));
        assertThat(users.findById(ana.id()).orElseThrow().mustChangePassword()).isFalse();
    }

    @Test
    void anAdminWithAPendingTemporaryPasswordLosesTheManagementEvenWithTheOldToken()
            throws Exception {
        Session ana = newAdmin();
        Session bruno = newAdmin();
        Session target = register(uniqueEmail("alvo-bruno"));

        temporaryPasswordFor(ana, bruno.id());

        assertThat(claimsOf(bruno.access())).doesNotContain("pwd_change");
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", bruno.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sem permissão"))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Você não tem mais acesso à gestão de usuários."));
        mockMvc.perform(
                        patch("/api/v1/admin/users/" + ana.id())
                                .header("Authorization", bruno.bearer())
                                .contentType(APPLICATION_JSON)
                                .content("{\"role\":\"CANDIDATE\"}"))
                .andExpect(status().isForbidden());
        reset(bruno, ana.id())
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Você não tem mais acesso à gestão de usuários."));
        reset(bruno, target.id()).andExpect(status().isForbidden());

        assertThat(users.findById(ana.id()).orElseThrow().canManageUsers()).isTrue();
        assertThat(users.findById(target.id()).orElseThrow().mustChangePassword()).isFalse();
        refresh(target.refresh()).andExpect(status().isOk());
    }

    @Test
    void anAccountWithoutPasswordGetsOneAndTheSocialLoginAlsoAsksForTheChange() throws Exception {
        Session admin = newAdmin();
        String email = uniqueEmail("social");
        String providerId = "g-" + UUID.randomUUID();
        SocialLoginCommand google =
                new SocialLoginCommand(SocialProvider.GOOGLE, providerId, email, "Social", null);
        UUID id =
                sessionOf(
                                body(
                                        mockMvc.perform(
                                                        post("/api/v1/auth/exchange")
                                                                .contentType(APPLICATION_JSON)
                                                                .content(
                                                                        "{\"code\":\""
                                                                                + loginWithSocialAccount
                                                                                        .execute(
                                                                                                google)
                                                                                        .value()
                                                                                + "\"}"))
                                                .andExpect(status().isOk())))
                        .id();

        String temporary = temporaryPasswordFor(admin, id);

        login(email, temporary)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.hasPassword").value(true))
                .andExpect(jsonPath("$.user.mustChangePassword").value(true));
        Session social =
                sessionOf(
                        body(
                                mockMvc.perform(
                                                post("/api/v1/auth/exchange")
                                                        .contentType(APPLICATION_JSON)
                                                        .content(
                                                                "{\"code\":\""
                                                                        + loginWithSocialAccount
                                                                                .execute(google)
                                                                                .value()
                                                                        + "\"}"))
                                        .andExpect(status().isOk())
                                        .andExpect(
                                                jsonPath("$.user.mustChangePassword")
                                                        .value(true))));
        assertBlocked(questions(social));
    }

    @Test
    void theOwnAccountIsRefused() throws Exception {
        Session admin = newAdmin();

        reset(admin, admin.id())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Você não pode alterar a sua própria conta por aqui."));
        assertThat(users.findById(admin.id()).orElseThrow().mustChangePassword()).isFalse();
    }

    @Test
    void onlyAnActiveAdminResetsAndOnlyAnExistingAccount() throws Exception {
        Session candidate = register(uniqueEmail("candidato"));
        Session victim = register(uniqueEmail("alvo"));

        mockMvc.perform(post("/api/v1/admin/users/" + victim.id() + "/password-reset"))
                .andExpect(status().isUnauthorized());
        reset(candidate, victim.id()).andExpect(status().isForbidden());
        assertThat(users.findById(victim.id()).orElseThrow().mustChangePassword()).isFalse();

        Session admin = newAdmin();
        reset(admin, UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuário não encontrado."));

        Session other = newAdmin();
        mockMvc.perform(
                        patch("/api/v1/admin/users/" + admin.id())
                                .header("Authorization", other.bearer())
                                .contentType(APPLICATION_JSON)
                                .content("{\"role\":\"CANDIDATE\"}"))
                .andExpect(status().isOk());
        reset(admin, victim.id())
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Você não tem mais acesso à gestão de usuários."));
        refresh(victim.refresh()).andExpect(status().isOk());
    }
}
