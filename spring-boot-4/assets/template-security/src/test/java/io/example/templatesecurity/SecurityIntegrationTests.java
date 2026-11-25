package io.example.templatesecurity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.example.templatesecurity.integration.PartnerInventoryClient;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "management.health.mail.enabled=false",
            "demo.user.username=demo@example.test",
            "demo.user.password=test-password",
            "spring.security.oauth2.client.registration.company.client-id=test-client",
            "spring.security.oauth2.client.registration.company.client-secret=test-secret",
            "spring.security.oauth2.client.provider.company.authorization-uri=https://idp.example.test/oauth2/authorize",
            "spring.security.oauth2.client.provider.company.token-uri=https://idp.example.test/oauth2/token",
            "spring.security.oauth2.client.provider.company.user-info-uri=https://idp.example.test/userinfo",
            "spring.security.oauth2.client.provider.company.user-name-attribute=sub",
            "spring.security.oauth2.client.provider.company.jwk-set-uri=https://idp.example.test/jwks",
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://idp.example.test",
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://idp.example.test/jwks",
            "spring.security.oauth2.resourceserver.jwt.audiences=template-security"
        })
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    private static HttpServer partnerServer;
    private static final AtomicReference<String> TOKEN_REQUEST = new AtomicReference<>();
    private static final AtomicReference<String> TOKEN_AUTHORIZATION = new AtomicReference<>();
    private static final AtomicReference<String> RESOURCE_AUTHORIZATION = new AtomicReference<>();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    JavaMailSender mailSender;

    @Autowired
    PartnerInventoryClient partnerInventoryClient;

    @DynamicPropertySource
    static void partnerProperties(DynamicPropertyRegistry registry) {
        startPartnerServer();
        String baseUrl = "http://localhost:" + partnerServer.getAddress().getPort();
        registry.add("spring.security.oauth2.client.registration.partner-inventory.client-id", () -> "partner-client");
        registry.add(
                "spring.security.oauth2.client.registration.partner-inventory.client-secret", () -> "partner-secret");
        registry.add("spring.security.oauth2.client.provider.partner.token-uri", () -> baseUrl + "/oauth/token");
        registry.add("partners.inventory.base-url", () -> baseUrl);
    }

    @AfterAll
    static void stopPartnerServer() {
        if (partnerServer != null) {
            partnerServer.stop(0);
        }
    }

    private static void startPartnerServer() {
        if (partnerServer != null) {
            return;
        }
        try {
            partnerServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            partnerServer.createContext("/oauth/token", exchange -> {
                TOKEN_AUTHORIZATION.set(exchange.getRequestHeaders().getFirst("Authorization"));
                TOKEN_REQUEST.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(
                        exchange,
                        "{\"access_token\":\"sample-access-token\",\"token_type\":\"Bearer\","
                                + "\"expires_in\":300,\"scope\":\"inventory.read\"}");
            });
            partnerServer.createContext("/api/stock", exchange -> {
                RESOURCE_AUTHORIZATION.set(exchange.getRequestHeaders().getFirst("Authorization"));
                respond(exchange, "{\"sku\":\"book-42\",\"available\":7}");
            });
            partnerServer.start();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    @Test
    void basicChainAuthenticatesThroughFilterAndRetainsChallenge() throws Exception {
        mvc.perform(get("/api/basic/report"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"template-security\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"));

        mvc.perform(get("/api/basic/report").with(httpBasic("demo@example.test", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value("http-basic"))
                .andExpect(jsonPath("$.subject").value("demo@example.test"));
    }

    @Test
    void basicChainDeniesRoleOutsideTheConfiguredAuthorities() throws Exception {
        mvc.perform(get("/api/basic/admin/report").with(httpBasic("demo@example.test", "test-password")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void resourceServerRequiresBearerAndEnforcesScope() throws Exception {
        mvc.perform(get("/api/resource/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));

        mvc.perform(get("/api/resource/reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_reports:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value("bearer"));

        mvc.perform(get("/api/resource/reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_reports:write"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void oauth2LoginAuthenticatesBrowserRequestThroughTheFilterChain() throws Exception {
        mvc.perform(get("/api/browser/me").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value("browser-session"))
                .andExpect(jsonPath("$.subject").value("user"));
    }

    @Test
    void clientCredentialsAddsApplicationTokenToOutboundRestClientRequest() {
        var stock = partnerInventoryClient.getStock();

        assertEquals(new PartnerInventoryClient.InventoryStock("book-42", 7), stock);
        assertEquals(
                "Basic " + Base64.getEncoder().encodeToString("partner-client:partner-secret".getBytes()),
                TOKEN_AUTHORIZATION.get());
        assertTrue(TOKEN_REQUEST.get().contains("grant_type=client_credentials"));
        assertTrue(TOKEN_REQUEST.get().contains("scope=inventory.read"));
        assertEquals("Bearer sample-access-token", RESOURCE_AUTHORIZATION.get());
    }

    @Test
    void stepUpRequiresBothPasswordAndOneTimeTokenFactors() throws Exception {
        mvc.perform(get("/api/browser/me").with(user("demo"))).andExpect(status().isOk());

        mvc.perform(get("/api/mfa/profile")
                        .with(user("demo").authorities(new SimpleGrantedAuthority("FACTOR_PASSWORD"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));

        mvc.perform(get("/api/mfa/profile")
                        .with(user("demo")
                                .authorities(
                                        new SimpleGrantedAuthority("FACTOR_PASSWORD"),
                                        new SimpleGrantedAuthority("FACTOR_OTT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access").value("mfa-profile"));
    }

    @Test
    void ottFilterDeliversGeneratedTokenThroughConfiguredMailSender() throws Exception {
        mvc.perform(post("/ott/generate").with(csrf()).param("username", "demo@example.test"))
                .andExpect(status().isAccepted());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
