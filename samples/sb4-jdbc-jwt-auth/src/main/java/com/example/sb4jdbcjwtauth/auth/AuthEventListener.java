package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.config.SecurityUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.context.event.EventListener;

@Component
public class AuthEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuthEventListener.class);

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof SecurityUser user) {
            log.info("Authentication succeeded for userId={}", user.id());
        } else {
            log.info("Authentication succeeded; mechanism={}",
                    event.getAuthentication().getClass().getSimpleName());
        }
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        log.info("Authentication failed; eventType={}", event.getClass().getSimpleName());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLifecycleEvent(AuthLifecycleEvent event) {
        log.info("Authentication lifecycle action={} userId={} occurredAt={}",
                event.action(), event.userId(), event.occurredAt());
    }
}
