package com.example.sb4oauth2login.config;

import com.example.sb4oauth2login.auth.LoginAuthenticationConverter;
import com.example.sb4oauth2login.auth.LoginAuthenticationFailureHandler;
import com.example.sb4oauth2login.auth.LoginAuthenticationSuccessHandler;
import com.example.sb4oauth2login.auth.OAuthLoginFailureHandler;
import com.example.sb4oauth2login.auth.OAuthLoginSuccessHandler;
import com.example.sb4oauth2login.auth.SecurityUser;
import com.example.sb4oauth2login.oauth.DatabaseAuthorizationRequestResolver;
import com.example.sb4oauth2login.user.InMemoryUserStore;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFilter;
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
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(InMemoryUserStore users) {
        return username -> users.findByUsername(username)
                .map(SecurityUser::new)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "Invalid credentials"));
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] bytes = Base64.getDecoder().decode(properties.secret());
        if (bytes.length < 32)
            throw new IllegalArgumentException("JWT_SECRET must be base64 encoded and at least 32 bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, JwtProperties properties, InMemoryUserStore users) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audience mismatch", null));
        OAuth2TokenValidator<Jwt> activeUser = jwt ->
                users.findById(jwt.getSubject()).filter(user -> user.active()).isPresent()
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Account unavailable", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), audience, activeUser));
        return decoder;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManager manager,
            JsonMapper mapper,
            LoginAuthenticationSuccessHandler loginSuccess,
            LoginAuthenticationFailureHandler loginFailure,
            OAuthLoginFailureHandler oauthFailure,
            OAuthLoginSuccessHandler oauthSuccess,
            SecurityProblemDetailHandler securityProblems,
            DatabaseAuthorizationRequestResolver authorizationRequestResolver)
            throws Exception {
        AuthenticationFilter loginFilter = new AuthenticationFilter(manager, new LoginAuthenticationConverter(mapper));
        loginFilter.setSuccessHandler(loginSuccess);
        loginFilter.setFailureHandler(loginFailure);
        loginFilter.setSecurityContextRepository(
                new org.springframework.security.web.context.NullSecurityContextRepository());
        return http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(securityProblems).accessDeniedHandler(securityProblems))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/",
                                "/index.html",
                                "/app.js",
                                "/theme.css",
                                "/style.css",
                                "/admin.html",
                                "/admin.js",
                                "/admin.css",
                                "/api/auth/login",
                                "/api/auth/oauth2/exchange",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/error")
                        .permitAll()
                        .requestMatchers("/api/auth/providers")
                        .permitAll()
                        .requestMatchers("/api/admin/**")
                        .hasAuthority("SCOPE_ADMIN")
                        .anyRequest()
                        .authenticated())
                .oauth2Login(oauth -> oauth.successHandler(oauthSuccess)
                        .failureHandler(oauthFailure)
                        .authorizationEndpoint(
                                endpoint -> endpoint.authorizationRequestResolver(authorizationRequestResolver)))
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(securityProblems)
                        .accessDeniedHandler(securityProblems))
                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
