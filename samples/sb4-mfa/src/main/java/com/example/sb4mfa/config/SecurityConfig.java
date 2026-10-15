package com.example.sb4mfa.config;

import com.example.sb4mfa.auth.MfaAuthenticationService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMultiFactorAuthentication(authorities = {})
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(User.withUsername("demo")
                .password(encoder.encode("changeit"))
                .roles("USER")
                .build());
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, MfaAuthenticationService mfaService, SecurityContextRepository contextRepository)
            throws Exception {
        return http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/",
                                "/index.html",
                                "/app.js",
                                "/styles.css",
                                "/assets/**",
                                "/error",
                                "/login",
                                "/logout",
                                "/api/csrf",
                                "/api/session",
                                "/webauthn/authenticate/options",
                                "/login/webauthn")
                        .permitAll()
                        .requestMatchers(
                                "/api/account/security",
                                "/api/mfa/totp/enroll",
                                "/api/mfa/totp/confirm",
                                "/api/mfa/wallet/enroll/**",
                                "/api/mfa/recovery-phrase/enroll",
                                "/webauthn/register/**")
                        .access((authentication, context) ->
                                new AuthorizationDecision(mfaService.isMfaComplete(authentication.get())
                                        && mfaService.hasRecentMfa(context.getRequest())))
                        .requestMatchers("/api/mfa/**")
                        .access((authentication, context) ->
                                new AuthorizationDecision(mfaService.hasPrimaryFactor(authentication.get())))
                        .anyRequest()
                        .access((authentication, context) ->
                                new AuthorizationDecision(mfaService.isMfaComplete(authentication.get()))))
                .formLogin(
                        form -> form.loginPage("/").loginProcessingUrl("/login").permitAll())
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .webAuthn(webAuthn -> webAuthn.rpName("Spring Boot MFA Demo")
                        .rpId("localhost")
                        .allowedOrigins("http://localhost:8080")
                        .disableDefaultRegistrationPage(true))
                .logout(Customizer.withDefaults())
                .build();
    }
}
