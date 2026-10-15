package com.example.sb4memoryjwt.config;

import com.example.sb4memoryjwt.auth.ApiAuthenticationConverter;
import com.example.sb4memoryjwt.auth.LoginAuthenticationFailureHandler;
import com.example.sb4memoryjwt.auth.LoginAuthenticationFilter;
import com.example.sb4memoryjwt.auth.LoginAuthenticationSuccessHandler;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.validation.Validator;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RSAKey jwtRsaKey(JwtProperties properties) {
        return Jwks.loadRsaKey(properties);
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey jwtRsaKey) {
        return Jwks.source(jwtRsaKey);
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey jwtRsaKey, JwtProperties properties) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtRsaKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Required audience is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), audienceValidator));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityProblemDetailHandler problemDetailHandler,
            AuthenticationManager authenticationManager,
            LoginAuthenticationSuccessHandler loginSuccessHandler,
            LoginAuthenticationFailureHandler loginFailureHandler,
            JsonMapper jsonMapper,
            Validator validator)
            throws Exception {
        LoginAuthenticationFilter loginFilter = new LoginAuthenticationFilter(
                authenticationManager,
                new ApiAuthenticationConverter(jsonMapper, validator),
                loginSuccessHandler,
                loginFailureHandler);
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/api/auth/register", "/api/auth/login", "/.well-known/jwks.json")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemDetailHandler)
                        .accessDeniedHandler(problemDetailHandler))
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
