package br.com.devready.api.career;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.api.TestJwts;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@TestPropertySource(properties = "devready.seed.enabled=true")
class CareerTest extends IntegrationTest {

    @Test
    void optionsArePublicAndOrdered() throws Exception {
        mockMvc.perform(get("/api/v1/career/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies.length()").value(10))
                .andExpect(jsonPath("$.companies[0].group").value("big-tech"))
                .andExpect(jsonPath("$.companies[0].slug").value("amazon"))
                .andExpect(jsonPath("$.companies[0].name").value("Amazon"))
                .andExpect(jsonPath("$.companies[5].group").value("tech-brasil"))
                .andExpect(jsonPath("$.companies[5].slug").value("ifood"))
                .andExpect(jsonPath("$.stacks.length()").value(8))
                .andExpect(jsonPath("$.stacks[0].area").value("backend"))
                .andExpect(jsonPath("$.stacks[0].slug").value("dotnet"))
                .andExpect(jsonPath("$.stacks[5].area").value("devops"))
                .andExpect(jsonPath("$.seniorities.length()").value(3))
                .andExpect(jsonPath("$.seniorities[0]").value("JUNIOR"));
    }

    @Test
    void optionsIgnoreAStaleOrBrokenBearer() throws Exception {
        String expired = "Bearer " + TestJwts.expired();

        mockMvc.perform(get("/api/v1/career/options").header("Authorization", expired))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies.length()").value(10));
        mockMvc.perform(
                        get("/api/v1/career/options")
                                .header("Authorization", "Bearer nao-e-um-jwt"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/career/profile").header("Authorization", expired))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        MockMvcRequestBuilders.put("/api/v1/career/profile")
                                .header("Authorization", expired)
                                .contentType(APPLICATION_JSON)
                                .content(
                                        "{\"companies\":[\"nubank\"],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileLifecycle() throws Exception {
        String access = registerAndGetAccessToken();
        mockMvc.perform(get("/api/v1/career/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/career/profile").header("Authorization", "Bearer " + access))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Perfil de carreira ainda não preenchido."));

        put(
                        access,
                        "{\"companies\":[\"nubank\",\"google\"],\"stack\":\"java-spring\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies.length()").value(2))
                .andExpect(jsonPath("$.companies[0].slug").value("nubank"))
                .andExpect(jsonPath("$.companies[0].group").value("tech-brasil"))
                .andExpect(jsonPath("$.companies[1].name").value("Google"))
                .andExpect(jsonPath("$.stack.slug").value("java-spring"))
                .andExpect(jsonPath("$.stack.name").value("Java e Spring"))
                .andExpect(jsonPath("$.stack.area").value("backend"))
                .andExpect(jsonPath("$.seniority").value("JUNIOR"));

        put(
                        access,
                        "{\"companies\":[\"ifood\",\"nubank\"],\"stack\":\"react\",\"seniority\":\"PLENO\"}")
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/career/profile").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies.length()").value(2))
                .andExpect(jsonPath("$.companies[0].slug").value("ifood"))
                .andExpect(jsonPath("$.companies[1].slug").value("nubank"))
                .andExpect(jsonPath("$.stack.slug").value("react"))
                .andExpect(jsonPath("$.stack.area").value("frontend"))
                .andExpect(jsonPath("$.seniority").value("PLENO"));
    }

    @Test
    void rejectsInvalidProfiles() throws Exception {
        String access = registerAndGetAccessToken();
        put(access, "{\"companies\":[],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("companies"))
                .andExpect(jsonPath("$.errors[0].message").value("Escolha de 1 a 5 empresas."));
        put(
                        access,
                        "{\"companies\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\"],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("companies"));
        put(access, "{\"stack\":\"\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(
                        jsonPath("$.errors[?(@.field == 'companies')].message")
                                .value("Escolha pelo menos uma empresa."))
                .andExpect(
                        jsonPath("$.errors[?(@.field == 'stack')].message")
                                .value("Escolha uma stack."))
                .andExpect(
                        jsonPath("$.errors[?(@.field == 'seniority')].message")
                                .value("Escolha a senioridade."));
        put(access, "{\"companies\":[\" \"],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("companies[0]"))
                .andExpect(jsonPath("$.errors[0].message").value("Empresa em branco na lista."));
        put(
                        access,
                        "{\"companies\":[\"nubank\",\"nubank\"],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Empresa repetida: nubank"));
        put(access, "{\"companies\":[\"xpto\"],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Empresa desconhecida: xpto."));
        put(access, "{\"companies\":[\"nubank\"],\"stack\":\"cobol\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Stack desconhecida: cobol."));
        put(access, "{\"companies\":[\"nubank\"],\"stack\":\"react\",\"seniority\":\"ESTAGIO\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.detail").value("Confira os campos informados."))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("seniority"))
                .andExpect(
                        jsonPath("$.errors[0].message")
                                .value("Valor inválido. Use um destes: JUNIOR, PLENO, SENIOR."));
        put(access, "{\"companies\":\"nubank\",\"stack\":\"react\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isBadRequest());
        put(access, "{\"companies\":[\"nubank\"").andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/career/profile").header("Authorization", "Bearer " + access))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsSlugsLongerThanTheColumns() throws Exception {
        String access = registerAndGetAccessToken();
        String longSlug = "a".repeat(51);

        put(
                        access,
                        "{\"companies\":[\"nubank\",\""
                                + longSlug
                                + "\"],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("companies[1]"))
                .andExpect(jsonPath("$.errors[0].message").value("Slug de empresa longo demais."));
        put(
                        access,
                        "{\"companies\":[\"nubank\"],\"stack\":\""
                                + longSlug
                                + "\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors[0].field").value("stack"))
                .andExpect(jsonPath("$.errors[0].message").value("Slug de stack longo demais."));

        String longestSlug = "a".repeat(50);
        put(
                        access,
                        "{\"companies\":[\""
                                + longestSlug
                                + "\"],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(
                        jsonPath("$.detail").value("Empresa desconhecida: " + longestSlug + "."));
        put(
                        access,
                        "{\"companies\":[\"nubank\"],\"stack\":\""
                                + longestSlug
                                + "\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.detail").value("Stack desconhecida: " + longestSlug + "."));
    }

    @Test
    void profilesAreIsolatedBetweenUsers() throws Exception {
        String anaToken = registerAndGetAccessToken();
        String brunoToken = registerAndGetAccessToken();

        put(
                        anaToken,
                        "{\"companies\":[\"nubank\"],\"stack\":\"java-spring\",\"seniority\":\"JUNIOR\"}")
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/api/v1/career/profile")
                                .header("Authorization", "Bearer " + brunoToken))
                .andExpect(status().isNotFound());

        put(
                        brunoToken,
                        "{\"companies\":[\"google\",\"ifood\"],\"stack\":\"react\",\"seniority\":\"SENIOR\"}")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/career/profile").header("Authorization", "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies.length()").value(1))
                .andExpect(jsonPath("$.companies[0].slug").value("nubank"))
                .andExpect(jsonPath("$.stack.slug").value("java-spring"))
                .andExpect(jsonPath("$.seniority").value("JUNIOR"));
        mockMvc.perform(
                        get("/api/v1/career/profile")
                                .header("Authorization", "Bearer " + brunoToken))
                .andExpect(jsonPath("$.companies[0].slug").value("google"))
                .andExpect(jsonPath("$.seniority").value("SENIOR"));
    }

    @Test
    void optionsAcceptsOnlyGet() throws Exception {
        mockMvc.perform(post("/api/v1/career/options")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/api/v1/career/options")
                                .header("Authorization", "Bearer " + registerAndGetAccessToken()))
                .andExpect(status().isMethodNotAllowed());
    }

    private ResultActions put(String access, String json) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put("/api/v1/career/profile")
                        .header("Authorization", "Bearer " + access)
                        .contentType(APPLICATION_JSON)
                        .content(json));
    }

    private String registerAndGetAccessToken() throws Exception {
        String body =
                mockMvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"c-"
                                                        + UUID.randomUUID()
                                                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
