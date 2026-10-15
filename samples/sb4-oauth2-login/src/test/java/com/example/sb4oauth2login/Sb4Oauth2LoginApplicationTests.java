package com.example.sb4oauth2login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb4oauth2login.auth.OAuthCodeStore;
import com.example.sb4oauth2login.auth.TokenResponse;
import com.example.sb4oauth2login.auth.TokenService;
import com.example.sb4oauth2login.oauth.DatabaseClientRegistrationRepository;
import com.example.sb4oauth2login.oauth.OAuthClientInput;
import com.example.sb4oauth2login.user.AppUser;
import com.example.sb4oauth2login.user.InMemoryUserStore;
import com.example.sb4oauth2login.user.OAuthUserMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        properties = {
            "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "app.security.demo-password=test-only-password",
            "app.security.admin-password=test-only-admin-password",
            "app.oauth2.client-secret-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "app.oauth2.allowed-hosts=idp.example.test"
        })
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class Sb4Oauth2LoginApplicationTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    OAuthCodeStore oauthCodes;

    @Autowired
    TokenService tokens;

    @Autowired
    OAuthUserMapper oauthUsers;

    @Autowired
    InMemoryUserStore users;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    DatabaseClientRegistrationRepository registrations;

    @BeforeEach
    void clearOAuthClients() {
        jdbc.sql("DELETE FROM oauth2_client").update();
    }

    @Test
    void localLoginIssuesJwtAcceptedByProtectedApi() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .servletPath("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"test-only-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        TokenResponse tokens = jsonMapper.readValue(login.getResponse().getContentAsString(), TokenResponse.class);
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("local-demo"))
                .andExpect(jsonPath("$.username").value("demo"));
    }

    @Test
    void invalidPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .servletPath("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://example.com/problems/invalid-credentials"));
    }

    @Test
    void protectedApiRejectsMissingAndInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.type").value("https://example.com/problems/unauthorized"));
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://example.com/problems/unauthorized"));
    }

    @Test
    void providerListIsPublicAndStartsEmpty() throws Exception {
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers").isEmpty());
    }

    @Test
    void oauthClientsAreManagedByAdminAndLoadedDynamically() throws Exception {
        String demoToken = login("demo", "test-only-password");
        mockMvc.perform(get("/api/admin/oauth-clients").header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isForbidden())
                .andExpect(header().string("WWW-Authenticate", "Bearer error=\"insufficient_scope\""))
                .andExpect(jsonPath("$.type").value("https://example.com/problems/forbidden"));

        String adminToken = login("admin", "test-only-admin-password");
        OAuthClientInput input = client("custom-idp", "Custom IdP", "original-secret", true);
        mockMvc.perform(post("/api/admin/oauth-clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientSecretConfigured").value(true))
                .andExpect(jsonPath("$.clientSecret").doesNotExist());
        mockMvc.perform(post("/api/admin/oauth-clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(input)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://example.com/problems/oauth-client-exists"));
        mockMvc.perform(post("/api/admin/oauth-clients/missing/test").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://example.com/problems/oauth-client-not-found"));

        var registration = registrations.findByRegistrationId("custom-idp");
        assertThat(registration).isNotNull();
        assertThat(registration.getClientSecret()).isEqualTo("original-secret");
        assertThat(registration.getProviderDetails().getAuthorizationUri())
                .isEqualTo("https://idp.example.test/authorize");
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[0].id").value("custom-idp"));
        mockMvc.perform(get("/oauth2/authorization/custom-idp")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/oauth2/authorization/custom-idp").with(request -> {
                    request.setServerName("other.example.test");
                    return request;
                }))
                .andExpect(status().is3xxRedirection());

        OAuthClientInput updated = client("custom-idp", "Renamed IdP", null, false);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/admin/oauth-clients/custom-idp")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecretConfigured").value(true))
                .andExpect(jsonPath("$.enabled").value(false));
        assertThat(registrations.findByRegistrationId("custom-idp")).isNull();
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers").isEmpty());

        mockMvc.perform(delete("/api/admin/oauth-clients/custom-idp").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidEnabledOAuthConfigurationIsRejectedWithoutSaving() throws Exception {
        String adminToken = login("admin", "test-only-admin-password");
        OAuthClientInput invalidOidc = new OAuthClientInput(
                "broken",
                "OIDC",
                "Broken",
                null,
                null,
                "id",
                "secret",
                "client_secret_basic",
                "{baseUrl}/login/oauth2/code/{registrationId}",
                "openid profile",
                "https://idp.example.test/authorize",
                "https://idp.example.test/token",
                "",
                "",
                "",
                "",
                Map.of(),
                null,
                null,
                null,
                true);
        mockMvc.perform(post("/api/admin/oauth-clients")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(invalidOidc)))
                .andExpect(status().isBadRequest());
        assertThat(registrations.findByRegistrationId("broken")).isNull();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .servletPath("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return jsonMapper
                .readValue(result.getResponse().getContentAsString(), TokenResponse.class)
                .accessToken();
    }

    private OAuthClientInput client(String id, String name, String secret, boolean enabled) {
        return new OAuthClientInput(
                id,
                "OAUTH2",
                name,
                null,
                null,
                "client-id",
                secret,
                "client_secret_basic",
                "{baseUrl}/login/oauth2/code/{registrationId}",
                "profile email",
                "https://idp.example.test/authorize",
                "https://idp.example.test/token",
                "https://idp.example.test/userinfo",
                "sub",
                "",
                "",
                Map.of(),
                null,
                null,
                null,
                enabled);
    }

    @Test
    void oauthExchangeIssuesJwtAndRejectsReplayedCode() throws Exception {
        AppUser user = oauthUsers.map("dex", externalPrincipal("exchange-subject"));
        String code = oauthCodes.issue(user);

        MvcResult exchange = mockMvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(java.util.Map.of("code", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        TokenResponse tokens = jsonMapper.readValue(exchange.getResponse().getContentAsString(), TokenResponse.class);
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.id()))
                .andExpect(jsonPath("$.username").value(user.username()));
        mockMvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(java.util.Map.of("code", code))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oauthExchangeRejectsUnknownCode() throws Exception {
        mockMvc.perform(post("/api/auth/oauth2/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"unknown-code\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedUserCannotUsePreviouslyIssuedJwt() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .servletPath("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"test-only-password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        TokenResponse token = jsonMapper.readValue(login.getResponse().getContentAsString(), TokenResponse.class);

        try {
            users.setActive("local-demo", false);
            mockMvc.perform(post("/api/auth/login")
                            .servletPath("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"demo\",\"password\":\"test-only-password\"}"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token.accessToken()))
                    .andExpect(status().isUnauthorized());
        } finally {
            users.setActive("local-demo", true);
        }
    }

    @Test
    void externalIdentityCanOnlyBeUnlinkedByItsOwnerAndOnlyWhenAnotherLoginRemains() throws Exception {
        AppUser dexUser = oauthUsers.map("dex", externalPrincipal("dex-subject"));
        AppUser googleUser = oauthUsers.map("google", externalPrincipal("google-subject"));
        String dexToken = tokens.issue(dexUser).accessToken();

        mockMvc.perform(delete("/api/me/external-identities/google").header("Authorization", "Bearer " + dexToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/me/external-identities/dex").header("Authorization", "Bearer " + dexToken))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + dexToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalProviders[0]").value("dex"));

        assertThat(googleUser.id()).isNotEqualTo(dexUser.id());
    }

    private DefaultOAuth2User externalPrincipal(String subject) {
        return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("sub", subject), "sub");
    }
}
