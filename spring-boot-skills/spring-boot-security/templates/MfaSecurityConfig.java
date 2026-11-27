package com.example.security.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authorization.AuthorizationManagerFactories;

@Configuration(proxyBeanMethods = false)
@EnableMultiFactorAuthentication(authorities = {})
public class MfaSecurityConfig {

    @Bean
    SecurityFilterChain browserSecurity(
            HttpSecurity http,
            OneTimeTokenGenerationSuccessHandler tokenDeliveryHandler) throws Exception {
        var recentPassword = AuthorizationManagerFactories.multiFactor()
            .requireFactor(factor -> factor
                .passwordAuthority()
                .validDuration(Duration.ofMinutes(30)))
            .build();

        var passwordAndOtt = AuthorizationManagerFactories.multiFactor()
            .requireFactors(
                FactorGrantedAuthority.PASSWORD_AUTHORITY,
                FactorGrantedAuthority.OTT_AUTHORITY)
            .build();

        return http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/payments/approve/**").access(recentPassword.hasRole("APPROVER"))
                .requestMatchers("/admin/**").access(passwordAndOtt.hasRole("ADMIN"))
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .oneTimeTokenLogin(ott -> ott
                .tokenGenerationSuccessHandler(tokenDeliveryHandler))
            .build();
    }
}
