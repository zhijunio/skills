package com.example.sb4oauth2login.auth;

import com.example.sb4oauth2login.config.JwtProperties;
import com.example.sb4oauth2login.user.AppUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenResponse issue(AppUser user) {
        if (!user.active()) {
            throw new IllegalArgumentException("Cannot issue a token for an inactive user");
        }
        Instant now = clock.instant();
        Instant expiry = now.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.id())
                .audience(List.of(properties.audience()))
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("username", user.username())
                .claim("name", user.displayName())
                .claim("email", user.email())
                .claim("scope", String.join(" ", user.roles()))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenResponse(token, "Bearer", expiry);
    }
}
