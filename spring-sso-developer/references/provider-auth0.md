# Auth0

**Verified against Auth0's current documentation on 2026-10-13.** Spring identifiers are Spring Security 7.1.0.

**A note on this file's depth.** The 2026-10-13 review verified common token-format and claim-mapping
behavior from Auth0's current documentation. Tenant-specific discovery metadata, Actions, audience,
RBAC settings, and account-linking policy still need confirmation from the actual tenant. Those are
listed below instead of inferred. See [providers.md](providers.md) for the cross-vendor comparison.

## The trailing slash

**Auth0's issuer ends with a slash.** From an Auth0 sample token:

```json
"iss": "https://my-domain.auth0.com/"
```

**This is the classic Auth0 integration failure**, because issuer validation is exact string comparison
and half the configuration examples in the world omit the slash. Symptoms: the signature verifies, the
token looks perfect, and validation fails with an issuer mismatch that reads as nonsense because the two
strings look identical at a glance.

Set `issuer-uri` to the form Auth0 actually emits, slash included, and where you have a custom domain use
the custom domain consistently. **Mixing the tenant domain and a custom domain between configuration and
tokens produces the same failure**, so pick one and use it everywhere.

## JWT or opaque, and why your resource server may reject everything

Auth0 issues both shapes, and a Spring resource server's JWT decoder can only handle one.

**What is verified:** "Access tokens issued for the Management API and access tokens issued for any
custom API that you have registered with Auth0 follow the JWT standard."

And, for Management API tokens specifically: "An access token issued for the Auth0 Management API should
be treated as opaque (regardless of whether it actually is), so you don't need to validate it." **Do not
build your own authorisation on Management API tokens**, which is the same advice as Okta's org
authorization server for the same reason.

**The token format depends on the requested audience.** Auth0 documents an opaque access token for the
`/userinfo` audience when no API audience is requested; a registered custom API audience produces a
JWT access token. The Management API token may also be a JWT, but Auth0 says to treat it as opaque.
If Spring reports a decoding or malformed-token error, first inspect the requested audience and the
registered API rather than weakening validation. See Auth0's [access token format guidance](https://auth0.com/docs/secure/tokens/access-tokens/get-access-tokens).

## Audience, which Auth0 makes explicit

Auth0's validation guidance requires that the `aud` claim "match the unique identifier of the target API
as defined in your API's Settings", and that the `scope` claim be checked against what the endpoint
requires.

That maps directly onto Spring:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://my-domain.auth0.com/
          audiences: https://api.example.com
```

**`audiences` is not validated unless you set it**, so an Auth0 integration that omits it accepts tokens
minted for any other API in the same tenant. In a product with several APIs behind one Auth0 tenant, that
is a confused-deputy problem rather than a hygiene issue. See
[security-oauth2-oidc.md](security-oauth2-oidc.md).

## Roles and permissions

Auth0 distinguishes requested API scopes, RBAC permissions, and assigned roles:

What can be said safely, and is enough to plan around:

- **API scopes appear in `scope`** and Spring maps them to `SCOPE_`-prefixed authorities by default.
- **The optional `permissions` claim** requires enabling RBAC and the API's “Add Permissions in the
  Access Token” setting. Spring does not treat this claim as `scope`; map it deliberately if the
  application authorizes from it.
- **Roles are not automatically included as a standard claim.** A Post-Login Action can read
  `event.authorization.roles` and add them to an ID token or access token as a custom claim. Use the
  token that matches the application role: an API must authorize from an access token, not an ID token.
- Use a public namespace URL you control, such as `https://id.example.com/roles`. Auth0 does not allow
  Auth0-owned domains as namespace identifiers. While private non-namespaced claims are allowed in
  some contexts, custom API access tokens cannot contain them; namespacing also avoids collisions with
  reserved claims such as `roles` and `permissions`. On collision, token issuance can succeed while the
  custom claim is omitted. Follow Auth0's current
  [custom claims guidance](https://auth0.com/docs/secure/tokens/json-web-tokens/create-custom-claims).

**Design advice that does not depend on the unverified parts:** prefer `scope` and permissions over
custom role claims where you can, because it is the path Spring handles natively and the one Auth0
documents most clearly.

## Not verified in this pass

Named precisely, so the gap is actionable:

- The exact **issuer and discovery metadata** for a specific tenant or custom domain. Read the tenant's
  `/.well-known/openid-configuration` and use its issuer and `jwks_uri` values verbatim.
- The precise **scope-to-authority mapping** for the tenant's API, including whether application code
  should use `scope` or Auth0's optional `permissions` claim.
- The specific Post-Login Action and token type used to emit custom roles for the application.
- **SAML** support specifics: metadata, certificate rotation, single logout.
- **SCIM** provisioning support and deprovisioning semantics.
- **PKCE for confidential clients**: whether Auth0 accepts it.
- **Subject identifier**: whether `sub` is stable and unique across connections, which matters because
  Auth0 aggregates several upstream connections and the same human can arrive through more than one.

That last one deserves emphasis even unverified: **Auth0 is frequently a broker in front of other
identity providers**, so a user signing in through two connections may present two identities. Establish
how your tenant handles account linking before keying records on anything.

## Version notes

| Concern | Detail |
| ------- | ------ |
| Verified | Issuer format with trailing slash; `/userinfo` opaque-token and custom-API JWT behavior by audience; optional RBAC `permissions` claim setting; Post-Login Action role claim mechanism; custom claim namespace guidance. Auth0 documentation, 2026-10-13 |
| Issuer | **Ends with a slash.** `https://{tenant}.auth0.com/` |
| Custom domains | Use one domain consistently across configuration and tokens |
| Management API tokens | Auth0 says to treat as opaque and not validate. Not a basis for your own authorisation |
| Remaining tenant-specific checks | Read discovery metadata and a decoded access token from the actual tenant; confirm claim names, API audience, enabled RBAC settings, roles and custom Actions |

## Gotchas

- Agent omits the trailing slash from the Auth0 issuer, and issuer validation fails against two strings
  that look identical
- Agent mixes the tenant domain and a custom domain between configuration and tokens, producing the same
  failure
- Agent builds authorisation on Management API tokens, which Auth0 says to treat as opaque
- Agent omits `audiences`, so tokens minted for a different API in the same tenant are accepted
- Agent sees a decode error rather than a validation error and debugs Spring, when the token is not a JWT
  and the cause is the API and audience configuration
- Agent invents a permissions claim name or a custom claim namespace. Neither was verified here; read
  Auth0's documentation
- Agent assumes one human is one identity when Auth0 is brokering several upstream connections
- Agent fills in this file's unverified sections from memory rather than from Auth0's documentation

## Related

- [providers.md](providers.md) · [spring-security-oidc.md](spring-security-oidc.md) · [security-oauth2-oidc.md](security-oauth2-oidc.md) · [checklist.md](checklist.md)
