package br.com.devready.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.domain.identity.User;
import com.jayway.jsonpath.JsonPath;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

class AdminUsersTest extends IntegrationTest {

    private static final String USERS = "/api/v1/admin/users";

    @Autowired private UserRepository users;

    private record Admin(UUID id, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String register(String email, String name) throws Exception {
        return mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"email\":\""
                                                + email
                                                + "\",\"password\":\"senha-forte\",\"displayName\":\""
                                                + name
                                                + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static UUID idOf(String body) {
        return UUID.fromString(JsonPath.read(body, "$.user.id"));
    }

    private ResultActions login(String email) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"senha-forte\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private void promote(UUID id) {
        User user = users.findById(id).orElseThrow();
        user.promoteToAdmin();
        users.save(user);
    }

    private Admin newAdmin() throws Exception {
        String email = unique("admin") + "@example.com";
        UUID id = idOf(register(email, "Admin"));
        promote(id);
        String body =
                login(email)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return new Admin(id, JsonPath.read(body, "$.accessToken"));
    }

    private ResultActions list(Admin admin, String query) throws Exception {
        return mockMvc.perform(get(USERS + query).header("Authorization", admin.bearer()));
    }

    private ResultActions change(Admin admin, UUID target, String json) throws Exception {
        return mockMvc.perform(
                patch(USERS + "/" + target)
                        .header("Authorization", admin.bearer())
                        .contentType(APPLICATION_JSON)
                        .content(json));
    }

    @Test
    void withoutATokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(USERS)).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        patch(USERS + "/" + UUID.randomUUID())
                                .contentType(APPLICATION_JSON)
                                .content("{\"active\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aCandidateIsForbidden() throws Exception {
        String body = register(unique("candidata") + "@example.com", "Candidata");
        String bearer = "Bearer " + JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get(USERS).header("Authorization", bearer))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        patch(USERS + "/" + idOf(body))
                                .header("Authorization", bearer)
                                .contentType(APPLICATION_JSON)
                                .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsNewestFirstWithSearchFiltersAndPages() throws Exception {
        Admin admin = newAdmin();
        String tag = unique("lista");
        UUID first = idOf(register(unique("p") + "@example.com", "Primeira " + tag));
        UUID second = idOf(register(unique("s") + "@example.com", "Segunda " + tag));
        UUID third = idOf(register(tag + "-terceira@example.com", "Terceira"));
        promote(second);
        User inactive = users.findById(third).orElseThrow();
        inactive.deactivate();
        users.save(inactive);
        String search = "?q=" + tag.toUpperCase(Locale.ROOT);

        list(admin, search + "&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(third.toString()))
                .andExpect(jsonPath("$.items[0].displayName").value("Terceira"))
                .andExpect(jsonPath("$.items[0].email").value(tag + "-terceira@example.com"))
                .andExpect(jsonPath("$.items[0].avatarUrl").value(nullValue()))
                .andExpect(jsonPath("$.items[0].role").value("CANDIDATE"))
                .andExpect(jsonPath("$.items[0].active").value(false))
                .andExpect(jsonPath("$.items[0].hasPassword").value(true))
                .andExpect(jsonPath("$.items[0].socialProviders").isEmpty())
                .andExpect(jsonPath("$.items[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.items[1].id").value(second.toString()));
        list(admin, search + "&size=2&page=2")
                .andExpect(jsonPath("$.items[0].id").value(first.toString()))
                .andExpect(jsonPath("$.items.length()").value(1));
        list(admin, search + "&size=2&page=9").andExpect(jsonPath("$.page").value(2));
        list(admin, search + "&role=ADMIN")
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(second.toString()));
        list(admin, search + "&active=false")
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(third.toString()));
        list(admin, search + "&role=CANDIDATE&active=true")
                .andExpect(jsonPath("$.items[0].id").value(first.toString()));
        list(admin, "?role=ROOT").andExpect(status().isBadRequest());
    }

    @Test
    void deactivatingEndsTheSessionsAndBlocksTheLoginUntilReactivated() throws Exception {
        Admin admin = newAdmin();
        String email = unique("desativada") + "@example.com";
        String body = register(email, "Desativada");
        UUID id = idOf(body);
        String refreshToken = JsonPath.read(body, "$.refreshToken");

        change(admin, id, "{\"active\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.role").value("CANDIDATE"));

        refresh(refreshToken).andExpect(status().isUnauthorized());
        login(email)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Conta desativada"));

        change(admin, id, "{\"active\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        change(admin, id, "{\"active\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        login(email).andExpect(status().isOk());
    }

    @Test
    void theNewRoleComesWithTheNextToken() throws Exception {
        Admin admin = newAdmin();
        String email = unique("promovida") + "@example.com";
        String body = register(email, "Promovida");
        UUID id = idOf(body);
        String oldBearer = "Bearer " + JsonPath.read(body, "$.accessToken");

        change(admin, id, "{\"role\":\"ADMIN\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(get(USERS).header("Authorization", oldBearer))
                .andExpect(status().isForbidden());
        String fresh =
                JsonPath.read(
                        login(email)
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.accessToken");
        mockMvc.perform(get(USERS).header("Authorization", "Bearer " + fresh))
                .andExpect(status().isOk());

        change(admin, id, "{\"role\":\"CANDIDATE\",\"active\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void anAdminDoesNotChangeTheirOwnAccountHere() throws Exception {
        Admin admin = newAdmin();

        change(admin, admin.id(), "{\"active\":false}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Você não pode alterar a sua própria conta por aqui."));
        change(admin, admin.id(), "{\"role\":\"CANDIDATE\"}")
                .andExpect(status().isUnprocessableContent());

        assertThat(users.findById(admin.id()).orElseThrow().isActiveAdmin()).isTrue();
    }

    @Test
    void anUnknownUserIsNotFound() throws Exception {
        change(newAdmin(), UUID.randomUUID(), "{\"active\":false}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Usuário não encontrado."));
    }

    @Test
    void aBodyWithNothingToChangeIsABadRequest() throws Exception {
        Admin admin = newAdmin();
        UUID id = idOf(register(unique("vazio") + "@example.com", "Vazio"));

        change(admin, id, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Informe o que mudar: a situação da conta, o papel ou os dois."));
        change(admin, id, "{\"active\":null}").andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownRoleIsAFieldProblem() throws Exception {
        Admin admin = newAdmin();
        UUID id = idOf(register(unique("papel") + "@example.com", "Papel"));

        change(admin, id, "{\"role\":\"ROOT\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("role"));
    }

    @Test
    void anAdminDemotedMeanwhileLosesTheListAndTheChangesAtOnce() throws Exception {
        Admin ana = newAdmin();
        Admin bruno = newAdmin();
        UUID helper = idOf(register(unique("ajudante") + "@example.com", "Ajudante"));
        change(ana, bruno.id(), "{\"role\":\"CANDIDATE\"}").andExpect(status().isOk());

        list(bruno, "")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sem permissão"))
                .andExpect(
                        jsonPath("$.detail")
                                .value("Você não tem mais acesso à gestão de usuários."));
        change(bruno, ana.id(), "{\"role\":\"CANDIDATE\"}")
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Você não tem mais acesso à gestão de usuários."));
        change(bruno, helper, "{\"role\":\"ADMIN\"}").andExpect(status().isForbidden());

        assertThat(users.findById(ana.id()).orElseThrow().isActiveAdmin()).isTrue();
        assertThat(users.findById(helper).orElseThrow().isAdmin()).isFalse();
    }

    @Test
    void anAdminDeactivatedMeanwhileLosesTheListAndTheChangesAtOnce() throws Exception {
        Admin ana = newAdmin();
        Admin bruno = newAdmin();
        change(ana, bruno.id(), "{\"active\":false}").andExpect(status().isOk());

        list(bruno, "?role=ADMIN")
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.detail")
                                .value("Você não tem mais acesso à gestão de usuários."));
        change(bruno, ana.id(), "{\"active\":false}").andExpect(status().isForbidden());

        assertThat(users.findById(ana.id()).orElseThrow().isActiveAdmin()).isTrue();
    }
}
