package com.example.sb4memoryjwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb4memoryjwt.auth.TokenResponse;
import com.example.sb4memoryjwt.config.JwtProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class Sb4MemoryJwtApplicationTests {
    private static final TestKeys TEST_KEYS = createTestKeys();

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    JwtProperties jwtProperties;

    @Autowired
    Clock clock;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.public-key", () -> TEST_KEYS.publicKey().toUri().toString());
        registry.add("app.jwt.private-key", () -> TEST_KEYS.privateKey().toUri().toString());
    }

    @Test
    void registersLogsInAndAccessesProtectedEndpointWithJwt() throws Exception {
        String username = "user-" + UUID.randomUUID();
        String credentials =
                objectMapper.writeValueAsString(Map.of("username", username, "password", "long-enough-password"));

        restTestClient
                .post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentials)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.username")
                .isEqualTo(username);

        TokenResponse tokenResponse = restTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentials)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(TokenResponse.class)
                .returnResult()
                .getResponseBody();
        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.tokenType()).isEqualTo("Bearer");

        restTestClient
                .get()
                .uri("/api/me")
                .headers(headers -> headers.setBearerAuth(tokenResponse.accessToken()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.username")
                .isEqualTo(username);
    }

    @Test
    void protectsProfileEndpointWithoutBearerTokenUsingProblemDetails() {
        restTestClient
                .get()
                .uri("/api/me")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Authentication required");
    }

    @Test
    void publishesPublicJwkWithoutPrivateKeyMaterial() {
        restTestClient
                .get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.keys[0].kid")
                .isEqualTo("local-rsa-key");
        restTestClient
                .get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectBody()
                .jsonPath("$.keys[0].d")
                .doesNotExist();
    }

    @Test
    void invalidCredentialsReturnProblemDetails() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "not-registered-" + UUID.randomUUID(), "password", "long-enough-password"));

        restTestClient
                .post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Authentication failed");
    }

    @Test
    void rejectsTokenWithWrongAudience() {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject("alice")
                .audience(List.of("another-client"))
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(5)))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(jwtProperties.keyId())
                .build();
        String wrongAudienceToken =
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        restTestClient
                .get()
                .uri("/api/me")
                .headers(headers -> headers.setBearerAuth(wrongAudienceToken))
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void rejectsInvalidRegistrationInput() throws Exception {
        String body =
                objectMapper.writeValueAsString(Map.of("username", "alice-" + UUID.randomUUID(), "password", "short"));

        restTestClient
                .post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }

    private static TestKeys createTestKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            Path directory = Path.of("target", "test-keys");
            Files.createDirectories(directory);
            Path publicKey = directory.resolve("public.pem");
            Path privateKey = directory.resolve("private.pem");
            writePem(publicKey, "PUBLIC KEY", keyPair.getPublic().getEncoded());
            writePem(privateKey, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
            return new TestKeys(publicKey, privateKey);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void writePem(Path path, String label, byte[] encoded) throws Exception {
        String body = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encoded);
        Files.writeString(path, "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n");
    }

    private record TestKeys(Path publicKey, Path privateKey) {}
}
