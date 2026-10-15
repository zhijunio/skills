package com.example.sb4oauth2login.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {
    private final OAuthCodeStore codes;
    private final TokenService tokens;

    public AuthController(OAuthCodeStore codes, TokenService tokens) {
        this.codes = codes;
        this.tokens = tokens;
    }

    @PostMapping("/api/auth/oauth2/exchange")
    TokenResponse exchange(@Valid @RequestBody OAuthExchangeRequest request) {
        return codes.consume(request.code())
                .map(tokens::issue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录凭证无效或已过期"));
    }
}
