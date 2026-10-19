package com.example.actuatordemo.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "management.server.port=0",
            "app.security.business.username=business",
            "app.security.business.password=business-test-secret",
            "app.security.management.username=operator",
            "app.security.management.password=management-test-secret"
        })
class SecurityIsolationIntegrationTest {

    private static final String BUSINESS_USER = "business";
    private static final String BUSINESS_PASSWORD = "business-test-secret";
    private static final String MANAGEMENT_USER = "operator";
    private static final String MANAGEMENT_PASSWORD = "management-test-secret";
    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int applicationPort;

    @Value("${local.management.port}")
    private int managementPort;

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Test
    void actuatorMatcherIsFirstAndClaimsManagementRoutesBeforeTheApplicationChain() {
        List<SecurityFilterChain> chains = filterChainProxy.getFilterChains();
        MockHttpServletRequest actuatorRequest = request("/actuator/health/liveness");
        MockHttpServletRequest businessRequest = request("/api/orders");

        assertThat(chains).hasSizeGreaterThanOrEqualTo(3);
        assertThat(chains.get(0).matches(actuatorRequest)).isTrue();
        assertThat(chains.get(1).matches(actuatorRequest)).isFalse();
        assertThat(chains.get(1).matches(businessRequest)).isTrue();

        HttpResult actuatorOnApplicationPort = get(applicationPort, "/actuator/health/liveness", null, null);
        assertProblem(actuatorOnApplicationPort, 401, "Authentication Required");
    }

    @Test
    void onlyLivenessAndReadinessAreAnonymousAndTheyRevealNoHealthDetails() {
        for (String probe : List.of("liveness", "readiness")) {
            HttpResult response = get(managementPort, "/actuator/health/" + probe, null, null);
            assertThat(response.status()).isEqualTo(200);
            assertThat(response.body()).contains("UP").doesNotContain("components", "details");
        }

        HttpResult aggregateHealth = get(managementPort, "/actuator/health", null, null);
        assertProblem(aggregateHealth, 401, "Authentication Required");
    }

    @Test
    void businessAndManagementAuthoritiesAreNotInterchangeable() {
        HttpResult anonymousBusiness = get(applicationPort, "/api/orders", null, null);
        assertProblem(anonymousBusiness, 401, "Authentication Required");

        HttpResult businessOrders = get(applicationPort, "/api/orders", BUSINESS_USER, BUSINESS_PASSWORD);
        assertThat(businessOrders.status()).isEqualTo(200);
        assertThat(businessOrders.body()).contains("ORD-1001");

        HttpResult businessOnManagement = get(managementPort, "/actuator/health", BUSINESS_USER, BUSINESS_PASSWORD);
        assertProblem(businessOnManagement, 403, "Forbidden");

        HttpResult managementOnBusiness = get(applicationPort, "/api/orders", MANAGEMENT_USER, MANAGEMENT_PASSWORD);
        assertProblem(managementOnBusiness, 403, "Forbidden");

        HttpResult managementHealth = get(managementPort, "/actuator/health", MANAGEMENT_USER, MANAGEMENT_PASSWORD);
        assertThat(managementHealth.status()).isEqualTo(200);
        assertThat(managementHealth.body()).doesNotContain("components", "details");
    }

    @Test
    void managementEndpointsOutsideExposureAllowlistAreNotPublished() {
        for (String endpoint : List.of("metrics", "env", "configprops", "mappings")) {
            HttpResult anonymous = get(managementPort, "/actuator/" + endpoint, null, null);
            assertProblem(anonymous, 401, "Authentication Required");

            HttpResult response = get(managementPort, "/actuator/" + endpoint, MANAGEMENT_USER, MANAGEMENT_PASSWORD);
            assertThat(response.status())
                    .as(endpoint + " should not be exposed")
                    .isIn(403, 404);
        }
    }

    @Test
    void publicBusinessStatusDoesNotGrantAccessToProtectedBusinessResources() {
        HttpResult publicStatus = get(applicationPort, "/api/public/status", null, null);
        assertThat(publicStatus.status()).isEqualTo(200);

        HttpResult protectedOrders = get(applicationPort, "/api/orders", null, null);
        assertProblem(protectedOrders, 401, "Authentication Required");
    }

    private HttpResult get(int port, String path, String username, String password) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET();
        if (username != null) {
            String credentials = java.util.Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            request.header("Authorization", "Basic " + credentials);
        }
        try {
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new HttpResult(
                    response.statusCode(),
                    response.headers().firstValue("Content-Type").orElse(""),
                    response.headers().firstValue("WWW-Authenticate").orElse(""),
                    response.body());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request was interrupted", e);
        }
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    private void assertProblem(HttpResult response, int status, String title) {
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body()).contains("\"type\"", "\"title\":\"" + title + "\"", "\"status\":" + status);
        if (status == 401) {
            assertThat(response.wwwAuthenticate()).startsWith("Basic");
        }
    }

    private record HttpResult(int status, String contentType, String wwwAuthenticate, String body) {}
}
