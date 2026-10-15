package com.example.sb4jwt.config;

import com.nimbusds.jose.jwk.RSAKey;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {
    private final List<RSAKey> publishedKeys;

    public JwksController(RSAKey activeKey) {
        this.publishedKeys = List.of(activeKey);
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> jwks() {
        return Jwks.publicSet(publishedKeys);
    }
}
