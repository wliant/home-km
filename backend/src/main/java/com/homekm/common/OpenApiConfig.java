package com.homekm.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI homekmOpenAPI(AppProperties props) {
        // Pin the server URL so the generated spec is deterministic — without
        // this, springdoc auto-derives it from the live request, which makes
        // the OpenApiContractTest baseline depend on the random test port.
        return new OpenAPI()
                .info(new Info()
                        .title(props.getName() + " API")
                        .version("v1")
                        .description("Home KM REST API. All authenticated endpoints require an `Authorization: Bearer <jwt>` header."))
                .servers(List.of(new Server().url("/").description("Same-origin (proxied via the frontend nginx)")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
