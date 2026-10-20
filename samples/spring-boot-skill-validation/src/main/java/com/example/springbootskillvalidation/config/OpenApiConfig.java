package com.example.springbootskillvalidation.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Task Management API",
                        version = "1.0",
                        description = "Task CRUD, filtering, pagination, and version-aware updates."))
class OpenApiConfig {}
