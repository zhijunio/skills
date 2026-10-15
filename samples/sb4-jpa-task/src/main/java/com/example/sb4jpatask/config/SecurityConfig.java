package com.example.sb4jpatask.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityProblemDetailHandler securityProblemDetailHandler(JsonMapper jsonMapper) {
        return new SecurityProblemDetailHandler(jsonMapper);
    }

    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http, SecurityProblemDetailHandler problemDetailHandler)
            throws Exception {
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
                        .authenticationEntryPoint(problemDetailHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemDetailHandler)
                        .accessDeniedHandler(problemDetailHandler))
                .build();
    }
}
