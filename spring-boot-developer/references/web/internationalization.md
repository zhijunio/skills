# Internationalization

Internationalization (i18n) is the handling of language and regional presentation. Keep message lookup, locale resolution and formatting consistent across MVC pages, validation errors and APIs. Store instants in a timezone-independent form; format them for the user's locale and zone at the presentation boundary.

## Message bundles

Spring Boot auto-configures `MessageSource` when the default bundle exists. Include `messages.properties` even when every supported locale has its own file:

```text
src/main/resources/
  messages.properties       # required fallback bundle
  messages_en.properties
  messages_zh_CN.properties
```

Keep message keys stable and describe meaning (`task.status.done`), not page position (`label.3`). Use parameterized messages and let framework interpolation handle values. Never build localized sentences by concatenating translated fragments.

Provide complete locale-specific message patterns for plurals and grammatical differences; English singular/plural rules do not generalize to every language. Use locale-aware formatters for numbers, dates, times and currencies, and pass an explicit currency rather than inferring one from locale. Use a message-formatting library with documented plural rules when `MessageFormat` is not expressive enough; do not implement plural selection with nested template conditionals.

```yaml
spring:
  messages:
    basename: messages
    encoding: UTF-8
    fallback-to-system-locale: false
```

Avoid replacing Boot's `MessageSource` bean unless the application needs custom resolution. `use-code-as-default-message` is useful for development diagnostics but can hide missing translations in production.

## Locale selection

Choose and document one locale policy. For browser-facing MVC, use `Accept-Language` negotiation or a user preference stored in a profile/cookie; do not silently trust the server's operating-system locale. For APIs, `Accept-Language` is usually clearer than a custom query parameter. Normalize and allowlist supported locales, and define the fallback explicitly.

If users can select a language, document precedence between their saved preference and `Accept-Language`, and use one resolver consistently across MVC, validation and error responses. A locale switch should accept only supported locale tags and should not become an open redirect; redirect to a validated local path.

Locale and timezone are separate preferences. A locale determines language and regional formatting conventions; a timezone determines how an instant appears on a clock. Store timestamps as `Instant` or another UTC-normalized instant and apply the user's zone only for display. Avoid changing the JVM default timezone per request.

## Validation, Thymeleaf and API errors

Use message keys in Bean Validation constraints and supply translations for both the constraint and any custom domain validation messages. Thymeleaf resolves `#{...}` messages and Spring MVC binding errors through `MessageSource`; keep form DTO property names and message keys aligned. See [thymeleaf.md](thymeleaf.md) and [validation.md](validation.md).

For APIs, keep machine-readable error types/codes stable. If `ProblemDetail.detail` is localized, resolve it from the request locale without changing the type URI or application error code. Document which fields may vary by `Accept-Language` and keep a deterministic fallback. See [error-handling.md](error-handling.md).

## Testing

Test each supported locale and the fallback locale, including message parameters, validation messages, date/number formatting and missing-key behavior. Avoid tests that inherit the developer machine's locale; set the request locale explicitly. Test locale selection at the MVC boundary and test message lookup separately where that is sufficient.

## Gotchas

- Agent adds only `messages_zh_CN.properties` and expects Boot to auto-configure `MessageSource` - provide the default `messages.properties` bundle too.
- Agent relies on the host's default locale - define a supported-locale allowlist and fallback.
- Agent treats locale and timezone as one setting - resolve and test them independently.
- Agent concatenates translated fragments - use complete parameterized messages.
- Agent assumes every language uses English singular/plural rules - provide locale-appropriate complete patterns and test zero/one/many cases.
- Agent formats money using locale alone - format with an explicit currency and locale.
- Agent has multiple locale resolvers for pages, validation and API errors - define one precedence policy and use it consistently.
- Agent accepts arbitrary locale-switch redirect targets - allow only supported locales and validated local redirects.
- Agent changes stable API error codes when translating `ProblemDetail` - localize human text while preserving machine-readable identifiers.
- Agent relies on the machine's locale in tests - set the locale explicitly.

## Related

- [thymeleaf.md](thymeleaf.md) · [validation.md](validation.md) · [error-handling.md](error-handling.md) · [configuration.md](../configuration/configuration.md)

## References

- [Spring Boot: Internationalization](https://docs.spring.io/spring-boot/reference/features/internationalization.html)
- [Unicode MessageFormat 2.0](https://github.com/unicode-org/message-format-wg)
