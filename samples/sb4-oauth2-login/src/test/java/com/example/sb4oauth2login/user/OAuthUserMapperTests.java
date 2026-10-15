package com.example.sb4oauth2login.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class OAuthUserMapperTests {
    private final InMemoryUserStore users =
            new InMemoryUserStore(PasswordEncoderFactories.createDelegatingPasswordEncoder(), "test-password");
    private final OAuthUserMapper mapper = new OAuthUserMapper(users);

    @Test
    void mapsStableProviderSubjectAndDoesNotLinkAccountsByEmail() {
        AppUser dexUser = mapper.map("dex", principal("subject-42", "same@example.test", "Dex Name"));
        AppUser sameDexUser = mapper.map("dex", principal("subject-42", "changed@example.test", "Updated Name"));
        AppUser googleUser = mapper.map("google", principal("subject-42", "same@example.test", "Google Name"));

        assertThat(sameDexUser.id()).isEqualTo(dexUser.id());
        assertThat(googleUser.id()).isNotEqualTo(dexUser.id());
        assertThat(dexUser.displayName()).isEqualTo("Dex Name");
        assertThat(dexUser.email()).isEqualTo("same@example.test");
    }

    @Test
    void refreshesProvidedProfileValuesAndKeepsValuesMissingFromLaterLogin() {
        AppUser original = mapper.map("dex", principal("subject-8", "first@example.test", "First Name"));
        var partialPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "subject-8", "name", "Updated Name"),
                "sub");

        AppUser updated = mapper.map("dex", partialPrincipal);

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.displayName()).isEqualTo("Updated Name");
        assertThat(updated.email()).isEqualTo("first@example.test");
        assertThat(users.findByUsername(updated.username())).contains(updated);
    }

    @Test
    void usesProviderUsernameWhenDisplayNameIsAbsent() {
        var principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "subject-7", "preferred_username", "alice"),
                "sub");

        AppUser user = mapper.map("dex", principal);

        assertThat(user.displayName()).isEqualTo("alice");
        assertThat(user.email()).isEmpty();
    }

    @Test
    void keepsProviderAndSubjectAsSeparateIdentityParts() {
        AppUser first = users.findOrCreateExternal("provider:one", "subject", "First", "");
        AppUser second = users.findOrCreateExternal("provider", "one:subject", "Second", "");

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(second.username()).isNotEqualTo(first.username());
        assertThat(users.findByUsername(first.username())).contains(first);
        assertThat(users.findByUsername(second.username())).contains(second);
    }

    @Test
    void inactiveUserCannotSignInAgainAndCannotUnlinkItsOnlyLoginMethod() {
        AppUser user = mapper.map("dex", principal("subject-90", "user@example.test", "User"));
        assertThat(users.setActive(user.id(), false)).isEqualTo(InMemoryUserStore.ActiveResult.UPDATED);

        assertThatThrownBy(() -> mapper.map("dex", principal("subject-90", "user@example.test", "User")))
                .isInstanceOf(OAuthUserMappingException.class);
        assertThat(users.unlinkExternalIdentity(user.id(), "dex"))
                .isEqualTo(InMemoryUserStore.UnlinkResult.LAST_LOGIN_METHOD);
        assertThat(users.externalProviders(user.id())).containsExactly("dex");
    }

    @Test
    void canDeactivateAndReactivateLocalUser() {
        AppUser demo = users.findByUsername("demo").orElseThrow();

        assertThat(users.setActive(demo.id(), false)).isEqualTo(InMemoryUserStore.ActiveResult.UPDATED);
        assertThat(users.findByUsername("demo"))
                .get()
                .extracting(AppUser::active)
                .isEqualTo(false);
        assertThat(users.setActive(demo.id(), true)).isEqualTo(InMemoryUserStore.ActiveResult.UPDATED);
        assertThat(users.findByUsername("demo"))
                .get()
                .extracting(AppUser::active)
                .isEqualTo(true);
    }

    private DefaultOAuth2User principal(String subject, String email, String name) {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", subject, "email", email, "name", name),
                "sub");
    }
}
