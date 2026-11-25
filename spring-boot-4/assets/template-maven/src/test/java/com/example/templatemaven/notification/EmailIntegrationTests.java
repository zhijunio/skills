package com.example.templatemaven.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.templatemaven.MailpitTestcontainersConfiguration;
import com.example.templatemaven.PostgresTestcontainersConfiguration;
import com.example.templatemaven.notification.application.EmailNotificationService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

@SpringBootTest
@AutoConfigureMockMvc
@Import({PostgresTestcontainersConfiguration.class, MailpitTestcontainersConfiguration.class})
class EmailIntegrationTests {

    @Autowired
    EmailNotificationService emailNotificationService;

    @Autowired
    Environment environment;

    @Test
    void sendsMessageThroughSmtpAndMailpitCapturesIt() throws Exception {
        var recipient = "mailpit-" + java.util.UUID.randomUUID() + "@example.test";
        emailNotificationService.sendWelcome(recipient, "Mailpit User");

        var mailpitBaseUrl = environment.getRequiredProperty("mailpit.base-url");
        var client = HttpClient.newHttpClient();
        var listRequest = HttpRequest.newBuilder(URI.create(mailpitBaseUrl + "/api/v1/messages"))
                .GET()
                .build();
        var listResponse = client.send(listRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, listResponse.statusCode());
        var messages = com.jayway.jsonpath.JsonPath.<List<Map<String, Object>>>read(listResponse.body(), "$.messages");
        var message = messages.stream()
                .filter(summary -> summary.toString().contains(recipient))
                .findFirst()
                .orElseThrow();

        var messageId = message.get("ID");
        var messageRequest = HttpRequest.newBuilder(URI.create(mailpitBaseUrl + "/api/v1/message/" + messageId))
                .GET()
                .build();
        var messageResponse = client.send(messageRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, messageResponse.statusCode());
        assertTrue(messageResponse.body().contains("Welcome, Mailpit User!"));
    }
}
