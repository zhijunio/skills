package com.example.sb4jpatask.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI taskManagerApi(BuildProperties buildProperties) {
        Info info = new Info()
                .title("Task Manager API")
                .version(buildProperties.getVersion())
                .description("Create tasks, browse the task list, and move tasks through their lifecycle. "
                        + "API access requires a JWT with the tasks:read or tasks:write scope.");

        return new OpenAPI().info(info)
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
