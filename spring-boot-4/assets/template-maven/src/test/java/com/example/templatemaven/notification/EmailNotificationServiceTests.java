package com.example.templatemaven.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.templatemaven.notification.application.EmailNotificationService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class EmailNotificationServiceTests {

    @Test
    void sendsWelcomeMessageWithConfiguredSenderAndRequestedRecipient() throws Exception {
        var mailSender = mock(JavaMailSender.class);
        var mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        var service = new EmailNotificationService(mailSender, "no-reply@example.test");

        service.sendWelcome("ada@example.com", "Ada");

        var messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        var message = messageCaptor.getValue();
        assertEquals("no-reply@example.test", message.getFrom()[0].toString());
        assertEquals("ada@example.com", message.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
        assertEquals("Welcome", message.getSubject());
        assertEquals("Welcome, Ada!", message.getContent());
    }
}
