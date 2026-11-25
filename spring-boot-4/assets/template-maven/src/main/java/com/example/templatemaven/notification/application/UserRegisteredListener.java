package com.example.templatemaven.notification.application;

import com.example.templatemaven.user.domain.UserRegistered;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class UserRegisteredListener {

    private final EmailNotificationService emailNotificationService;

    UserRegisteredListener(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegistered event) {
        emailNotificationService.sendWelcome(event.email(), event.displayName());
    }
}
