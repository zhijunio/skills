package com.example.sb4oauth2login.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb4oauth2login.user.InMemoryUserStore;
import com.example.sb4oauth2login.user.OAuthUserMapper;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class OAuthLoginSuccessHandlerTests {
    @Test
    void redirectsToGenericFailureWhenProviderReturnsBlankSubject() throws Exception {
        InMemoryUserStore users =
                new InMemoryUserStore(PasswordEncoderFactories.createDelegatingPasswordEncoder(), "test-password");
        OAuthLoginSuccessHandler handler =
                new OAuthLoginSuccessHandler(new OAuthUserMapper(users), new OAuthCodeStore(Clock.systemUTC()));
        var principal =
                new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("sub", ""), "sub");
        var authentication = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "dex");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo(OAuthLoginFailureHandler.FAILURE_REDIRECT_URL);
    }
}
