package com.example.sb4oauth2login.oauth;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/oauth-clients")
public class OAuthClientAdminController {
    private final OAuthClientService clients;
    private final OAuthClientTemplateStore templates;

    public OAuthClientAdminController(OAuthClientService clients, OAuthClientTemplateStore templates) {
        this.clients = clients;
        this.templates = templates;
    }

    @GetMapping
    List<OAuthClientSummary> findAll() {
        return clients.findAll();
    }

    @GetMapping("/templates")
    List<OAuthClientTemplateSummary> findTemplates() {
        return templates.findAll();
    }

    @GetMapping("/audit")
    List<OAuthClientAuditRecord> findRecentAudit() {
        return clients.findRecentAudit();
    }

    @PostMapping("/{registrationId}/test")
    OAuthClientConnectionResult testConnection(@PathVariable String registrationId) {
        return clients.testConnection(registrationId);
    }

    @PostMapping
    ResponseEntity<OAuthClientSummary> create(
            @Valid @RequestBody OAuthClientInput input, @AuthenticationPrincipal Jwt jwt) {
        OAuthClientSummary client = clients.create(input, actor(jwt));
        return ResponseEntity.created(URI.create("/api/admin/oauth-clients/" + client.registrationId()))
                .body(client);
    }

    @PutMapping("/{registrationId}")
    OAuthClientSummary update(
            @PathVariable String registrationId,
            @Valid @RequestBody OAuthClientInput input,
            @AuthenticationPrincipal Jwt jwt) {
        return clients.update(registrationId, input, actor(jwt))
                .orElseThrow(() -> new OAuthClientNotFoundException(registrationId));
    }

    @DeleteMapping("/{registrationId}")
    ResponseEntity<Void> delete(@PathVariable String registrationId, @AuthenticationPrincipal Jwt jwt) {
        if (!clients.delete(registrationId, actor(jwt))) {
            throw new OAuthClientNotFoundException(registrationId);
        }
        return ResponseEntity.noContent().build();
    }

    private String actor(Jwt jwt) {
        String username = jwt.getClaimAsString("username");
        return username == null || username.isBlank() ? jwt.getSubject() : username;
    }
}
