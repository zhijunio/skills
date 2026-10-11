package com.example.taskmanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI taskManagerApi(BuildProperties buildProperties) {
        return new OpenAPI().info(new Info()
                .title("Task Manager API")
                .version(buildProperties.getVersion())
                .description("Create tasks, browse the task list, and move tasks through their lifecycle."));
    }
}
