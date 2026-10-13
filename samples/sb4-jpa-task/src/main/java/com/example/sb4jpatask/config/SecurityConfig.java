package com.example.sb4jpatask.config;

import java.io.IOException;
import java.net.URI;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/actuator/**").hasAuthority("SCOPE_ops:read")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(problemDetailEntryPoint(jsonMapper)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemDetailEntryPoint(jsonMapper))
                        .accessDeniedHandler(problemDetailAccessDeniedHandler(jsonMapper)))
                .build();
    }

    private AuthenticationEntryPoint problemDetailEntryPoint(JsonMapper jsonMapper) {
        return (request, response, exception) -> writeProblem(jsonMapper, response, HttpStatus.UNAUTHORIZED,
                "unauthorized", "Authentication is required to access this resource.");
    }

    private AccessDeniedHandler problemDetailAccessDeniedHandler(JsonMapper jsonMapper) {
        return (request, response, exception) -> writeProblem(jsonMapper, response, HttpStatus.FORBIDDEN,
                "forbidden", "You do not have permission to perform this action.");
    }

    private void writeProblem(JsonMapper jsonMapper, jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status, String type, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://example.com/problems/" + type));
        problem.setTitle(status.getReasonPhrase());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
