package com.vivek.docorganizer.controller;

import com.vivek.docorganizer.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the API documentation: the spec must be reachable without a token and must actually
 * describe every endpoint the README advertises.
 */
class OpenApiDocsTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("the OpenAPI spec is public and documents every endpoint")
    void openApiSpecIsServed() throws Exception {

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Smart Digital Document Organizer API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/me'].get").exists())
                .andExpect(jsonPath("$.paths['/api/documents'].get").exists())
                .andExpect(jsonPath("$.paths['/api/documents/upload'].post").exists())
                .andExpect(jsonPath("$.paths['/api/documents/stats'].get").exists())
                .andExpect(jsonPath("$.paths['/api/documents/tags'].get").exists())
                .andExpect(jsonPath("$.paths['/api/documents/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/documents/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/documents/{id}/download'].get").exists());
    }

    @Test
    @DisplayName("Swagger UI is reachable at /swagger-ui.html without a token")
    void swaggerUiIsReachable() throws Exception {

        // springdoc serves the page by forwarding /swagger-ui.html to the bundled UI.
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
