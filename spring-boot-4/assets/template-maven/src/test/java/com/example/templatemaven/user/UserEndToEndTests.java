package com.example.templatemaven.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.templatemaven.PostgresTestcontainersConfiguration;
import com.example.templatemaven.RedisTestcontainersConfiguration;
import com.example.templatemaven.notification.application.EmailNotificationService;
import com.example.templatemaven.user.api.CreateUserRequest;
import com.example.templatemaven.user.api.UserResponse;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({PostgresTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class})
class UserEndToEndTests {

    @Autowired
    RestTestClient restTestClient;

    @MockitoBean
    EmailNotificationService emailNotificationService;

    @Test
    void createsAndReadsUserThroughTheRunningHttpServer() {
        var email = "e2e-" + UUID.randomUUID() + "@example.com";

        var created = restTestClient
                .post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateUserRequest(email, "End to end user"))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(UserResponse.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(created);
        assertEquals(email.toLowerCase(Locale.ROOT), created.email());
        assertEquals(0, created.version());

        restTestClient
                .get()
                .uri("/api/users/{id}", created.id())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.email")
                .isEqualTo(email.toLowerCase(Locale.ROOT));
    }
}
