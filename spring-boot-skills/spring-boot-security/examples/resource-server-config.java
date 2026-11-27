// GOOD: stateless resource server with scope-based request rules.

import com.example.auth.config.SecurityErrorHandler;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

    @Bean
    SecurityErrorHandler securityErrorHandler(tools.jackson.databind.json.JsonMapper jsonMapper) {
        return new SecurityErrorHandler(jsonMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityErrorHandler errors) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").hasAuthority("SCOPE_read")
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/**").hasAuthority("SCOPE_write")
                .requestMatchers("/api/v1/admin/**").hasAuthority("SCOPE_admin")
                .anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(errors.authenticationEntryPoint())
                .accessDeniedHandler(errors.accessDeniedHandler()))
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(errors.authenticationEntryPoint())
                .accessDeniedHandler(errors.accessDeniedHandler())
                .jwt(Customizer.withDefaults()))
            .build();
    }
}
