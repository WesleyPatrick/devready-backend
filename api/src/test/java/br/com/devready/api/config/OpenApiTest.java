package br.com.devready.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import org.junit.jupiter.api.Test;

class OpenApiTest extends IntegrationTest {

    @Test
    void openApiDocumentIsPublicAndDescribesAuthEndpoints() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists())
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/career/options'].get.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/career/profile'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/career/profile'].get").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/career/profile'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/career/track'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/career/track'].get.security").doesNotExist())
                .andExpect(
                        jsonPath("$.paths['/api/v1/career/track/competencies/{slug}'].get")
                                .exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/career/track/competencies/{slug}'].get.security")
                                .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/questions'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/questions'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/questions/daily'].get").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/questions/daily'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/questions/{slug}'].get").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/questions/{slug}'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/questions/{slug}/answer'].post").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/questions/{slug}/answer'].post.security")
                                .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/me/progress'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me/progress'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/challenges/{slug}'].get").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/challenges/{slug}'].get.security")
                                .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/challenges/{slug}/runs'].post").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/challenges/{slug}/runs'].post.security")
                                .doesNotExist())
                .andExpect(
                        jsonPath("$.paths['/api/v1/challenges/{slug}/submissions'].post").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/challenges/{slug}/submissions'].get").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/challenges/{slug}/submissions'].get.security")
                                .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/lessons'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/simulations'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/current'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sessions'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{id}'].get").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/sessions/{id}/questions/{position}'].put")
                                .exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/sessions/{id}/challenges/{position}/runs'].post")
                                .exists())
                .andExpect(
                        jsonPath(
                                        "$.paths['/api/v1/sessions/{id}/challenges/{position}/submissions'].post")
                                .exists())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{id}/finish'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/sessions/{id}/result'].get").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/sessions/{id}/result'].get.security")
                                .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/me/achievements'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/submissions/{id}'].get").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/submissions/{id}'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}'].patch").exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/admin/users/{id}'].patch.security")
                                .doesNotExist())
                .andExpect(
                        jsonPath("$.paths['/api/v1/admin/users/{id}/password-reset'].post")
                                .exists())
                .andExpect(
                        jsonPath("$.paths['/api/v1/admin/users/{id}/password-reset'].post.security")
                                .doesNotExist());
    }

    @Test
    void swaggerUiIsReachable() throws Exception {
        mockMvc.perform(get("/api/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
