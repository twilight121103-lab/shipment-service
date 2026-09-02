package com.example.logistics.infrastructure.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * <p>The API is documented at {@code /swagger-ui.html}. Authentication is described as
 * an OAuth2 authorization-code flow pointing at the local Keycloak, so users can
 * obtain a bearer token directly from the UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shipmentOpenApi(@Value("${app.openapi.oauth.token-url:http://localhost:8000/realms/logistics/protocol/openid-connect/token}")
                                   String tokenUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("Shipment Service API")
                        .description("""
                                Logistics platform shipment management.
                                Roles: LOGISTICS_USER (create/cancel own), LOGISTICS_OPERATOR (view/status),
                                LOGISTICS_ADMIN (full access). Use the Idempotency-Key header to make
                                create operations idempotent.
                                """)
                        .version("v1")
                        .contact(new Contact().name("Logistics Platform")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .flows(new OAuthFlows().password(
                                        new OAuthFlow().tokenUrl(tokenUrl)))));
    }
}
