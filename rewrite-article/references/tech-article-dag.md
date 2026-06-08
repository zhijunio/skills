# Technical article — DAG order (optional)

Use when **tech-mastery Plan** or **rewrite-article** needs a default reader journey for **explainer / deep-dive** posts (not news, not release notes).

## 1. Default dependency order

```text
1. Problem / stakes        — why care
2. Language or user model  — spec / API contract (JLS, RFC)
3. Data structures         — headers, wire format, tables
4. Mechanism / algorithm   — steps, state machine
5. Examples                — code, traces
6. Edge cases              — downgrade, failure, limits
7. Version / migration     — JDK N+, flags, deprecation
8. Practice                — profiling, alternatives, when not to use
9. Further reading
```

**Rule:** do not put **§7 version** before **§4 mechanism** unless the post is *“what changed in JDK 21”* news-style.

## 2. Worked fix (version-drift explainer — generic)

**Before:** version / migration warning → spec overview → data format → mechanism → …

**After:** problem → spec / user model → data format → **mechanism** → examples → edge cases → **version table** → practice → links

**Why:** readers need the **stable mental model** before «feature X is off by default in release N+» — same DAG rule for JVM, HTTP, DB protocols, etc.

## 3. When to use rewrite-article instead

- Only **order** is wrong, body mostly OK → **`rewrite-article`** confirmation outline.
- Order OK but **paragraphs long / wording** → **`humanize`**.

See also [`dag-reorder.md`](dag-reorder.md).
