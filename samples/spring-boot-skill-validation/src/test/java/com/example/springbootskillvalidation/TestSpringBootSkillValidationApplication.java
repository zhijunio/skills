package com.example.springbootskillvalidation;

import org.springframework.boot.SpringApplication;

public class TestSpringBootSkillValidationApplication {

    public static void main(String[] args) {
        SpringApplication.from(SpringBootSkillValidationApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
