package com.example.sb4jdbcjwtauth.config;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private final RSAKey jwtRsaKey;

    public JwksController(RSAKey jwtRsaKey) {
        this.jwtRsaKey = jwtRsaKey;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return Jwks.publicSet(jwtRsaKey);
    }
}
