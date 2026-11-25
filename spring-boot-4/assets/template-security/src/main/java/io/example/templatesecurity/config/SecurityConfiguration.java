package io.example.templatesecurity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMultiFactorAuthentication(authorities = {})
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService demoUserDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${demo.user.username}") String username,
            @Value("${demo.user.password}") String password) {
        var user = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("USER")
                .build();
        return new org.springframework.security.provisioning.InMemoryUserDetailsManager(user);
    }

    @Bean
    @Order(1)
    SecurityFilterChain basicApi(HttpSecurity http, SecurityProblemResponses problems) throws Exception {
        return http.securityMatcher("/api/basic/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/basic/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Basic realm=\"template-security\"");
                            problems.commence(request, response, exception);
                        })
                        .accessDeniedHandler(problems))
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain resourceServerApi(HttpSecurity http, SecurityProblemResponses problems) throws Exception {
        return http.securityMatcher("/api/resource/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/resource/reports/**")
                        .hasAuthority("SCOPE_reports:read")
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            problems.commence(request, response, exception);
                        })
                        .accessDeniedHandler(problems))
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationEntryPoint((request, response, exception) -> {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            problems.commence(request, response, exception);
                        })
                        .accessDeniedHandler(problems)
                        .jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain browserAndMfa(
            HttpSecurity http, EmailOneTimeTokenSuccessHandler tokenHandler, SecurityProblemResponses problems)
            throws Exception {
        var mfa = AuthorizationManagerFactories.multiFactor()
                .requireFactors(
                        org.springframework.security.core.authority.FactorGrantedAuthority.PASSWORD_AUTHORITY,
                        org.springframework.security.core.authority.FactorGrantedAuthority.OTT_AUTHORITY)
                .build();

        return http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/", "/actuator/health", "/actuator/info", "/favicon.ico", "/assets/**")
                        .permitAll()
                        .requestMatchers("/api/mfa/admin/**")
                        .access(mfa.hasRole("ADMIN"))
                        .requestMatchers("/api/mfa/**")
                        .access(mfa.authenticated())
                        .requestMatchers("/api/browser/**")
                        .authenticated()
                        .anyRequest()
                        .denyAll())
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(problems))
                .formLogin(Customizer.withDefaults())
                .oauth2Login(Customizer.withDefaults())
                .oneTimeTokenLogin(ott -> ott.tokenGenerationSuccessHandler(tokenHandler))
                .build();
    }
}
