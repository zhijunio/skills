package io.example.templatesecurity.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class EmailOneTimeTokenSuccessHandler implements OneTimeTokenGenerationSuccessHandler {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailOneTimeTokenSuccessHandler(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, OneTimeToken oneTimeToken)
            throws IOException, ServletException {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(oneTimeToken.getUsername());
        message.setSubject("Your one-time sign-in token");
        message.setText("Use this one-time token to sign in: " + oneTimeToken.getTokenValue());
        mailSender.send(message);
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
    }
}
