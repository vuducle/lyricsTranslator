package org.example.javamusicapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 💅 **Was geht hier ab?**
 * Diese Klasse pimpt unsere API-Doku. Sie nutzt OpenAPI (aka Swagger), um automatisch 'ne interaktive Webseite
 * zu generieren, auf der alle API-Endpunkte gelistet sind. Richtig lit, weil man da direkt testen kann,
 * welche Daten man senden muss und was zurückkommt.
 *
 * Außerdem fügt sie den "Authorize"-Button hinzu, damit man seinen JWT-Token eingeben und auch die
 * geschützten Endpunkte easy testen kann. Spart massiv Zeit, weil man nicht alles manuell
 * in Postman oder so reinhacken muss.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // Define a bearerAuth security scheme so Swagger UI shows the Authorize button
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(new Info()
                        .title("SpringBoot API")
                        .version("v1")
                        .description("API documentation for SpringBoot API - authentication endpoints included")
                        .contact(new Contact().name("Dev Team").email("dev@lyrics.app")));
    }
}
