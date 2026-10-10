package br.com.devready.api.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

@TestPropertySource(properties = "devready.seed.enabled=true")
class LearningTrackTest extends IntegrationTest {

    private static final String JUNIOR_JAVA =
            "{\"companies\":[\"amazon\",\"google\",\"nubank\",\"meta\"],"
                    + "\"stack\":\"java-spring\",\"seniority\":\"JUNIOR\"}";

    private static final List<String> JUNIOR_JAVA_ORDER =
            List.of(
                    "estruturas-de-dados-e-algoritmos",
                    "design-de-sistemas",
                    "sql-e-modelagem-de-dados",
                    "java",
                    "kotlin",
                    "python",
                    "spring",
                    "testes-automatizados",
                    "apis-rest",
                    "nuvem",
                    "mensageria-e-processamento-assincrono",
                    "observabilidade",
                    "git-e-fluxo-de-codigo",
                    "comunicacao-e-trabalho-em-equipe",
                    "ingles");

    @Test
    void trackAndCompetencyNeedAProfile() throws Exception {
        mockMvc.perform(get("/api/v1/career/track")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/career/track/competencies/java"))
                .andExpect(status().isUnauthorized());
        String access = registerAndGetAccessToken();

        track(access)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Perfil de carreira ainda não preenchido."));
        competency(access, "java")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Perfil de carreira ainda não preenchido."));
    }

    @Test
    void mergesTheTrackChosenForEachCompany() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        String body =
                track(access)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.plan.totalCompetencies").value(15))
                        .andExpect(jsonPath("$.plan.doneCompetencies").value(0))
                        .andExpect(jsonPath("$.plan.jobsAnalyzed").value(34))
                        .andExpect(jsonPath("$.plan.sample").doesNotExist())
                        .andExpect(jsonPath("$.plan.missingCompanies.length()").value(1))
                        .andExpect(jsonPath("$.plan.missingCompanies[0].slug").value("meta"))
                        .andExpect(jsonPath("$.plan.missingCompanies[0].name").value("Meta"))
                        .andExpect(jsonPath("$.plan.byDifficulty.EASY.total").value(5))
                        .andExpect(jsonPath("$.plan.byDifficulty.MEDIUM.total").value(8))
                        .andExpect(jsonPath("$.plan.byDifficulty.HARD.total").value(2))
                        .andExpect(jsonPath("$.plan.byDifficulty.HARD.done").value(0))
                        .andExpect(
                                jsonPath("$.competencies[0].name")
                                        .value("Estruturas de dados e algoritmos"))
                        .andExpect(jsonPath("$.competencies[0].category").value("fundamento"))
                        .andExpect(jsonPath("$.competencies[0].difficulty").value("MEDIUM"))
                        .andExpect(jsonPath("$.competencies[0].status").value("NOT_STARTED"))
                        .andExpect(jsonPath("$.competencies[0].requirement").value("obrigatoria"))
                        .andExpect(jsonPath("$.competencies[0].askedBy[0]").value("amazon"))
                        .andExpect(jsonPath("$.competencies[0].askedBy[1]").value("google"))
                        .andExpect(jsonPath("$.competencies[0].askedBy[2]").value("nubank"))
                        .andExpect(jsonPath("$.competencies[0].frequency.jobs").value(26))
                        .andExpect(jsonPath("$.competencies[0].frequency.total").value(34))
                        .andExpect(jsonPath("$.competencies[4].askedBy.length()").value(1))
                        .andExpect(jsonPath("$.competencies[4].askedBy[0]").value("nubank"))
                        .andExpect(jsonPath("$.competencies[4].requirement").value("obrigatoria"))
                        .andExpect(jsonPath("$.competencies[5].askedBy[0]").value("amazon"))
                        .andExpect(jsonPath("$.competencies[5].requirement").value("desejavel"))
                        .andExpect(jsonPath("$.competencies[5].frequency.jobs").value(3))
                        .andExpect(jsonPath("$.competencies[6].askedBy[0]").value("google"))
                        .andExpect(jsonPath("$.competencies[6].frequency.jobs").value(5))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        List<String> slugs = JsonPath.read(body, "$.competencies[*].slug");
        assertThat(slugs).containsExactlyElementsOf(JUNIOR_JAVA_ORDER);
    }

    @Test
    void planSummaryAgreesWithTheCompetencyList() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        String body =
                track(access)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<String> difficulties = JsonPath.read(body, "$.competencies[*].difficulty");
        for (String difficulty : List.of("EASY", "MEDIUM", "HARD")) {
            long listed = difficulties.stream().filter(difficulty::equals).count();
            Integer total = JsonPath.read(body, "$.plan.byDifficulty." + difficulty + ".total");
            assertThat(total).isEqualTo((int) listed);
        }
        Map<String, Integer> byDifficulty = JsonPath.read(body, "$.plan.byDifficulty");
        assertThat(byDifficulty).hasSize(3);
        Integer totalCompetencies = JsonPath.read(body, "$.plan.totalCompetencies");
        assertThat(difficulties).hasSize(totalCompetencies);

        Integer jobsAnalyzed = JsonPath.read(body, "$.plan.jobsAnalyzed");
        List<Integer> totals = JsonPath.read(body, "$.competencies[*].frequency.total");
        assertThat(totals).hasSize(totalCompetencies).containsOnly(jobsAnalyzed);
    }

    @Test
    void usesTheExactTrackWhenTheCompanyHasOne() throws Exception {
        String access =
                registerWithProfile(
                        "{\"companies\":[\"amazon\"],\"stack\":\"java-spring\",\"seniority\":\"PLENO\"}");

        track(access)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.jobsAnalyzed").value(61))
                .andExpect(jsonPath("$.plan.totalCompetencies").value(12))
                .andExpect(jsonPath("$.plan.missingCompanies.length()").value(0))
                .andExpect(jsonPath("$.competencies[4].slug").value("spring"))
                .andExpect(jsonPath("$.competencies[4].frequency.jobs").value(40))
                .andExpect(jsonPath("$.competencies[4].frequency.total").value(61));
    }

    @Test
    void trackIsEmptyWhenNoChosenCompanyHasOneInTheArea() throws Exception {
        String access =
                registerWithProfile(
                        "{\"companies\":[\"meta\",\"nubank\"],\"stack\":\"react\",\"seniority\":\"JUNIOR\"}");

        track(access)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.totalCompetencies").value(0))
                .andExpect(jsonPath("$.plan.jobsAnalyzed").value(0))
                .andExpect(jsonPath("$.plan.sample").doesNotExist())
                .andExpect(jsonPath("$.plan.byDifficulty.EASY.total").value(0))
                .andExpect(jsonPath("$.plan.missingCompanies[0].slug").value("meta"))
                .andExpect(jsonPath("$.plan.missingCompanies[1].slug").value("nubank"))
                .andExpect(jsonPath("$.plan.missingCompanies[1].name").value("Nubank"))
                .andExpect(jsonPath("$.competencies.length()").value(0));
    }

    @Test
    void competencyDetailBringsMaterialsQuestionsAndChallengesOfTheStackArea() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        competency(access, "estruturas-de-dados-e-algoritmos")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("estruturas-de-dados-e-algoritmos"))
                .andExpect(jsonPath("$.requirement").value("obrigatoria"))
                .andExpect(jsonPath("$.frequency.jobs").value(26))
                .andExpect(jsonPath("$.frequency.total").value(34))
                .andExpect(jsonPath("$.description", startsWith("Listas, mapas")))
                .andExpect(jsonPath("$.lockedReason").value(nullValue()))
                .andExpect(jsonPath("$.materials.length()").value(3))
                .andExpect(
                        jsonPath("$.materials[0].title")
                                .value("Projeto de Algoritmos, de Paulo Feofiloff"))
                .andExpect(jsonPath("$.materials[0].type").value("livro"))
                .andExpect(jsonPath("$.materials[0].language").value("pt"))
                .andExpect(jsonPath("$.materials[0].free").value(true))
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(
                        jsonPath("$.questions[0].slug")
                                .value("backend-junior-java-equals-hashcode"))
                .andExpect(jsonPath("$.questions[0].title").value("equals sem hashCode em HashMap"))
                .andExpect(jsonPath("$.questions[0].difficulty").value("HARD"))
                .andExpect(jsonPath("$.questions[0].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.challenges.length()").value(4))
                .andExpect(jsonPath("$.challenges[0].slug").value("valid-parentheses"))
                .andExpect(jsonPath("$.challenges[0].title").value("Parênteses balanceados"))
                .andExpect(jsonPath("$.challenges[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.challenges[0].languages[0]").value("JAVA"))
                .andExpect(jsonPath("$.challenges[0].languages[2]").value("JAVASCRIPT"))
                .andExpect(jsonPath("$.challenges[3].slug").value("merge-sorted-arrays"))
                .andExpect(jsonPath("$.challenges[3].difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.challenges[0].status").value("NOT_STARTED"));

        competency(access, "sql-e-modelagem-de-dados")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(4))
                .andExpect(
                        jsonPath("$.questions[0].title")
                                .value("Chave estrangeira inexistente no insert"))
                .andExpect(
                        jsonPath("$.questions[1].title").value("Pedidos só com cliente cadastrado"))
                .andExpect(jsonPath("$.questions[2].difficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.questions[3].difficulty").value("HARD"))
                .andExpect(jsonPath("$.challenges.length()").value(0));
    }

    @Test
    void competencyOutsideTheTrackIsNotFound() throws Exception {
        String access = registerWithProfile(JUNIOR_JAVA);

        competency(access, "react")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Competência fora da sua trilha: react."));
        competency(access, "nao-existe")
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.detail").value("Competência fora da sua trilha: nao-existe."));
    }

    private ResultActions track(String access) throws Exception {
        return mockMvc.perform(
                get("/api/v1/career/track").header("Authorization", "Bearer " + access));
    }

    private ResultActions competency(String access, String slug) throws Exception {
        return mockMvc.perform(
                get("/api/v1/career/track/competencies/" + slug)
                        .header("Authorization", "Bearer " + access));
    }

    private String registerWithProfile(String profile) throws Exception {
        String access = registerAndGetAccessToken();
        mockMvc.perform(
                        put("/api/v1/career/profile")
                                .header("Authorization", "Bearer " + access)
                                .contentType(APPLICATION_JSON)
                                .content(profile))
                .andExpect(status().isOk());
        return access;
    }

    private String registerAndGetAccessToken() throws Exception {
        String body =
                mockMvc.perform(
                                post("/api/v1/auth/register")
                                        .contentType(APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"t-"
                                                        + UUID.randomUUID()
                                                        + "@example.com\",\"password\":\"senha-forte\",\"displayName\":\"Ana\"}"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
