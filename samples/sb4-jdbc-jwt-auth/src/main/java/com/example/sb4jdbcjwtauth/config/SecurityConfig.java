package com.example.sb4jdbcjwtauth.config;

import com.example.sb4jdbcjwtauth.auth.ApiAuthenticationConverter;
import com.example.sb4jdbcjwtauth.auth.LoginAuthenticationFailureHandler;
import com.example.sb4jdbcjwtauth.auth.LoginAuthenticationFilter;
import com.example.sb4jdbcjwtauth.auth.LoginAuthenticationSuccessHandler;
import com.example.sb4jdbcjwtauth.auth.RefreshAuthenticationFailureHandler;
import com.example.sb4jdbcjwtauth.auth.RefreshAuthenticationFilter;
import com.example.sb4jdbcjwtauth.auth.RefreshAuthenticationSuccessHandler;
import com.example.sb4jdbcjwtauth.auth.RefreshTokenAuthenticationProvider;
import com.example.sb4jdbcjwtauth.auth.RefreshTokenLogoutHandler;
import com.example.sb4jdbcjwtauth.user.UserRepository;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

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
    JwtDecoder jwtDecoder(RSAKey jwtRsaKey, JwtProperties properties, SessionJwtValidator sessionValidator) {
        try {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtRsaKey.toRSAPublicKey())
                    .signatureAlgorithm(SignatureAlgorithm.RS256)
                    .build();
            OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(properties.audience())
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "Required audience is missing", null));
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    JwtValidators.createDefaultWithIssuer(properties.issuer()), audienceValidator,
                    sessionValidator));
            return decoder;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load RSA JWT public key", exception);
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email)
                .map(user -> new SecurityUser(user.id(), user.email(), user.password()))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    @Bean
    AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher events) {
        return new DefaultAuthenticationEventPublisher(events);
    }

    @Qualifier("loginAuthenticationManager")
    @Bean
    AuthenticationManager loginAuthenticationManager(UserDetailsService userDetailsService,
                                                     PasswordEncoder passwordEncoder,
                                                     AuthenticationEventPublisher eventPublisher) {
        var passwordProvider = new DaoAuthenticationProvider(userDetailsService);
        passwordProvider.setPasswordEncoder(passwordEncoder);
        var manager = new ProviderManager(passwordProvider);
        manager.setAuthenticationEventPublisher(eventPublisher);
        return manager;
    }

    @Qualifier("refreshAuthenticationManager")
    @Bean
    AuthenticationManager refreshAuthenticationManager(RefreshTokenAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("loginAuthenticationManager") AuthenticationManager loginAuthenticationManager,
            @Qualifier("refreshAuthenticationManager") AuthenticationManager refreshAuthenticationManager,
            ApiAuthenticationConverter authenticationConverter,
            RefreshTokenLogoutHandler refreshTokenLogoutHandler,
            SecurityProblemDetailHandler problemDetailHandler,
            LoginAuthenticationSuccessHandler loginSuccessHandler,
            LoginAuthenticationFailureHandler loginFailureHandler,
            RefreshAuthenticationSuccessHandler refreshSuccessHandler,
            RefreshAuthenticationFailureHandler refreshFailureHandler) throws Exception {
        var loginAuthenticationFilter = new LoginAuthenticationFilter(loginAuthenticationManager,
                authenticationConverter, loginSuccessHandler, loginFailureHandler);
        var refreshAuthenticationFilter = new RefreshAuthenticationFilter(refreshAuthenticationManager,
                authenticationConverter, refreshSuccessHandler, refreshFailureHandler);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                                "/api/auth/logout", "/.well-known/jwks.json", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(problemDetailHandler)
                        .accessDeniedHandler(problemDetailHandler))
                .logout(logout -> logout
                        .logoutRequestMatcher(requestMatcher("DELETE", "/api/auth/logout"))
                        .addLogoutHandler(refreshTokenLogoutHandler)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if (request.getAttribute(RefreshTokenLogoutHandler.INVALID_REQUEST_ATTRIBUTE) == null
                                    && !response.isCommitted()) {
                                response.setStatus(HttpStatus.NO_CONTENT.value());
                            }
                        }))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .addFilterBefore(loginAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(refreshAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static RequestMatcher requestMatcher(String method, String path) {
        return request -> method.equals(request.getMethod()) && path.equals(request.getServletPath());
    }
}
