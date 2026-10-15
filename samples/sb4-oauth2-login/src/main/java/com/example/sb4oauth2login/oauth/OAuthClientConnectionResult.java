package com.example.sb4oauth2login.oauth;

import java.time.Instant;
import java.util.List;

public record OAuthClientConnectionResult(
        String registrationId, boolean reachable, Instant testedAt, List<OAuthEndpointCheck> checks) {}
