package com.example.actuatordemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "app.security.business.username=business",
            "app.security.business.password=business-test-secret",
            "app.security.management.username=operator",
            "app.security.management.password=management-test-secret"
        })
class ActuatorSecurityDemoApplicationTests {

    @Test
    void contextLoads() {}
}
