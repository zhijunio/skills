package io.example.templatesecurity.api;

import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityDemoController {

    @GetMapping("/")
    Message home() {
        return new Message("public", "Welcome");
    }

    @GetMapping("/api/basic/report")
    Message basicReport(Principal principal) {
        return new Message("http-basic", principal.getName());
    }

    @GetMapping("/api/basic/admin/report")
    Message basicAdminReport(Principal principal) {
        return new Message("http-basic-admin", principal.getName());
    }

    @GetMapping("/api/resource/reports")
    Message resourceReport(Principal principal) {
        return new Message("bearer", principal.getName());
    }

    @GetMapping("/api/browser/me")
    Message browserIdentity(Principal principal) {
        return new Message("browser-session", principal.getName());
    }

    @GetMapping("/api/mfa/profile")
    Message mfaProfile(Principal principal) {
        return new Message("mfa-profile", principal.getName());
    }

    @GetMapping("/api/mfa/admin/report")
    Message mfaAdminReport(Principal principal) {
        return new Message("mfa-admin", principal.getName());
    }

    record Message(String access, String subject) {}
}
