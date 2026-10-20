package com.example.springbootskillvalidation.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

class TaskApiPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsConfiguredPageLimit() {
        contextRunner.withPropertyValues("app.tasks.max-page-size=75").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(TaskApiProperties.class).maxPageSize()).isEqualTo(75);
        });
    }

    @Test
    void rejectsPageLimitAboveAllowedMaximum() {
        contextRunner.withPropertyValues("app.tasks.max-page-size=101").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("maxPageSize");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TaskApiProperties.class)
    static class TestConfiguration {}
}
