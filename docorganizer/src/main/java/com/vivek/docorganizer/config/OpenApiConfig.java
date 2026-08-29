package com.vivek.docorganizer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI docOrganizerOpenApi() {

        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste the token returned by POST /api/auth/login.");

        return new OpenAPI()
                .info(new Info()
                        .title("Smart Digital Document Organizer API")
                        .version("v1")
                        .description("JWT-secured document storage: upload, tag, search, download "
                                + "and delete documents under a per-user storage quota.")
                        .contact(new Contact().name("Vivek Kumar"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
