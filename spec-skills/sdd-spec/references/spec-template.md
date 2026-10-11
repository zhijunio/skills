# <Title>

<!--
  Spec writing guide:
  - Goal: one sentence describing what must be true after this change.
  - Scope: explicit in-scope behaviors, modules, or interfaces. Keep it tight.
  - Non-goals: what this change deliberately excludes; prevents scope creep.
  - Current Context: **increment facts** for this change only (existing APIs,
    data formats, dependency versions, deployment model). Stable domain
    terminology belongs in optional `CONTEXT.md` (single domain) or
    `docs/context/<domain>/CONTEXT.md` (multi-domain) — link or
    reference shared terms here; do not paste the full glossary.
  - Requirements: numbered list of intended capabilities (narrative). Orient readers;
    Plan/Build bind only to Acceptance Criteria, not to Requirements alone.
  - Acceptance Criteria: each criterion gets a stable identifier (AC-1, AC-2...).
    Format: "When <precondition>, then <observable result>." Must be pass/fail.
    Every Requirement that must drive delivery needs at least one AC.
  - Constraints: technical limits and trade-offs for this change (compatibility,
    security, performance, migration). Prefer this for decisions scoped to this spec.
  - Decisions (optional): major trade-offs for this change only — chosen approach,
    rejected alternatives, and why. Omit when Constraints is enough.
  - Related ADRs (optional): links such as ADR-0003 for cross-feature architecture
    decisions. Do not paste ADR bodies here; use docs/adr/ when other specs must
    reference the same decision.
  - Optional CONTEXT (consumer projects): root `CONTEXT.md` for stable domain
    language; multi-domain projects add `docs/context/<domain>/CONTEXT.md`.
    Not required for small or single-spec projects.
  - Open Questions: unresolved decisions that must be answered before planning.
    Remove all open questions before the spec is approved.
-->

## Goal

<!-- One sentence. Example: "Users can reset their password via email without exposing account enumeration." -->

## Scope

<!-- What's included. Example: "POST /auth/forgot-password endpoint, email template, rate limiting at 3 requests per hour per email." -->

## Non-goals

<!-- What's explicitly excluded. Example: "Two-factor authentication, OAuth provider linking, in-app password change UI." -->

## Current Context

<!-- Increment facts for this change only — not the full domain glossary.
  Example: "Existing User model has email (unique, indexed). Mailer uses SendGrid."
  When terminology is stable across specs, define it in CONTEXT.md (or the domain
  file under `docs/context/<domain>/`) and reference it here instead of redefining. -->

## Requirements

<!--
  Numbered list of observable behaviors.
  Example:
  1. A user can request a password reset by providing their email address.
  2. The system sends a single-use, time-limited reset token to that email.
  3. The token expires after 1 hour and cannot be reused.
-->

## Acceptance Criteria

<!--
  Each criterion must be independently verifiable with a pass/fail result.
  Do not hide implementation steps here — those belong in the plan.

  Example:
  - AC-1: When POST /auth/forgot-password is called with a registered email,
    the response is always 200 with a generic confirmation message, regardless
    of whether the email exists.
  - AC-2: When a valid reset token is submitted to POST /auth/reset-password,
    the user's password is updated and the token is invalidated.
  - AC-3: When the same reset token is submitted a second time, the response
    is 400 with an error indicating the token has been used or expired.
-->

- AC-1:

## Constraints

<!--
  Technical limits and trade-offs for this change.
  Example:
  - Must not expose whether an email is registered (no enumeration).
  - Reset tokens must be cryptographically random, at least 32 bytes.
  - Email delivery must not block the HTTP response (>2s timeout degrades gracefully).
-->

## Decisions

<!--
  Optional. Use only for major trade-offs scoped to this spec.
  Omit this section when Constraints is enough.

  Example:
  - Chosen: rate limit at the API gateway (consistent with existing auth routes).
  - Rejected: per-endpoint limits (duplicates policy); Redis counter (new dependency).
-->

## Related ADRs

<!--
  Optional. Link cross-feature architecture decisions only — do not paste ADR text.
  Create docs/adr/0001-short-title.md when multiple specs must share one decision.

  Example:
  - ADR-0001: email delivery provider and failover model
-->

## Open Questions

<!--
  Unresolved decisions. Must be answered before planning.
  Example:
  - Should we reuse the existing mailer queue or send synchronously?
  - What is the minimum password complexity requirement?
-->

## Revision log

<!--
  Append-only after first approval. Leave empty for a new spec.
  Each entry: date | reason | changed ACs (or "none — clarification") | plan impact (yes/no + brief note).

  Example:
  - 2026-06-08 | Clarify AC-2 wording | none — clarification | no
  - 2026-06-09 | Raise AC-3 latency limit | AC-3 | no — return to sdd-build
-->
