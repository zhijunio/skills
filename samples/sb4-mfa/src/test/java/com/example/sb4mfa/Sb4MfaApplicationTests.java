package com.example.sb4mfa;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sb4mfa.auth.MfaAuthenticationService;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class Sb4MfaApplicationTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {}

    @Test
    void passwordLoginLeavesSessionPendingUntilASecondFactorIsVerified() throws Exception {
        MvcResult loginResult = mockMvc.perform(
                        post("/login").with(csrf()).param("username", "demo").param("password", "changeit"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.mfaComplete").value(false));
        mockMvc.perform(get("/api/secure").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/mfa/totp/enroll").session(session).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyPrimaryAndSecondFactorCombinationCanAccessProtectedResources() throws Exception {
        mockMvc.perform(get("/api/secure")
                        .with(user("demo")
                                .authorities(
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                "FACTOR_PASSWORD"),
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                "FACTOR_EMAIL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"));
    }

    @Test
    void ordinaryProfileReadDoesNotRequireFreshMfaButSecurityReadDoes() throws Exception {
        var completedMfa = user("demo")
                .authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("FACTOR_PASSWORD"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("FACTOR_EMAIL"));

        mockMvc.perform(get("/api/users/me").with(completedMfa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"));
        mockMvc.perform(get("/api/account/security").with(completedMfa)).andExpect(status().isForbidden());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                MfaAuthenticationService.MFA_VERIFIED_AT_SESSION_ATTRIBUTE,
                Instant.now().minus(Duration.ofMinutes(6)));
        mockMvc.perform(get("/api/account/security").session(session).with(completedMfa))
                .andExpect(status().isForbidden());

        session.setAttribute(MfaAuthenticationService.MFA_VERIFIED_AT_SESSION_ATTRIBUTE, Instant.now());
        mockMvc.perform(get("/api/account/security").session(session).with(completedMfa))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recent MFA verification is valid"));
    }

    @Test
    void recoveryPhraseEnrollmentRequiresRecentMfaAndPhraseCompletesTheSecondStep() throws Exception {
        var completedMfa = user("demo")
                .authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("FACTOR_PASSWORD"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("FACTOR_EMAIL"));

        mockMvc.perform(post("/api/mfa/recovery-phrase/enroll").with(csrf()).with(completedMfa))
                .andExpect(status().isForbidden());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(MfaAuthenticationService.MFA_VERIFIED_AT_SESSION_ATTRIBUTE, Instant.now());
        String body = mockMvc.perform(post("/api/mfa/recovery-phrase/enroll")
                        .session(session)
                        .with(csrf())
                        .with(completedMfa))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var matcher = Pattern.compile("\\\"phrase\\\":\\\"([^\\\"]+)\\\"").matcher(body);
        org.assertj.core.api.Assertions.assertThat(matcher.find()).isTrue();
        String phrase = matcher.group(1);

        mockMvc.perform(post("/api/mfa/recovery-phrase")
                        .session(session)
                        .with(csrf())
                        .with(user("demo")
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "FACTOR_PASSWORD")))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"phrase\":\"" + phrase + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaComplete").value(true));
    }
}
