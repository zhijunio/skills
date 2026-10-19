package com.example.actuatordemo.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String BUSINESS_AUTHORITY = "BUSINESS_READ";
    static final String MANAGEMENT_AUTHORITY = "ACTUATOR_ADMIN";

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(
            PasswordEncoder encoder,
            @Value("${app.security.business.username}") String businessUsername,
            @Value("${app.security.business.password}") String businessPassword,
            @Value("${app.security.management.username}") String managementUsername,
            @Value("${app.security.management.password}") String managementPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(businessUsername)
                        .password(encoder.encode(businessPassword))
                        .authorities(BUSINESS_AUTHORITY)
                        .build(),
                User.withUsername(managementUsername)
                        .password(encoder.encode(managementPassword))
                        .authorities(MANAGEMENT_AUTHORITY)
                        .build());
    }

    @Bean
    @Order(1)
    SecurityFilterChain actuatorChain(HttpSecurity http, SecurityProblemHandler problems) throws Exception {
        return http.securityMatcher("/actuator/**")
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/actuator/health/liveness", "/actuator/health/readiness")
                                .permitAll()
                                .anyRequest()
                                .hasAuthority(MANAGEMENT_AUTHORITY))
                .httpBasic(basic -> basic.authenticationEntryPoint(problems))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationChain(HttpSecurity http, SecurityProblemHandler problems) throws Exception {
        return http.securityMatcher("/api/**", "/", "/index.html", "/app.js", "/styles.css")
                .authorizeHttpRequests(auth -> auth.requestMatchers("/", "/index.html", "/app.js", "/styles.css")
                        .permitAll()
                        .requestMatchers("/api/public/**")
                        .permitAll()
                        .requestMatchers("/api/**")
                        .hasAuthority(BUSINESS_AUTHORITY)
                        .anyRequest()
                        .denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(problems))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain fallbackChain(HttpSecurity http, SecurityProblemHandler problems) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(problems).accessDeniedHandler(problems))
                .build();
    }
}
