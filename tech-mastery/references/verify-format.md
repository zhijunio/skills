# Verification format

Record in **`<slug>-mastery-map.md` → ## Verification** and mirror key fields in **`TOPIC.yaml`**.

## Dual runtime (common for JVM / versioned topics)

| Field | Meaning |
|-------|---------|
| `TOPIC.yaml` → `runtime` | **Narrative baseline** — spec edition, JDK you explain in prose (e.g. JDK 21, RFC 9110) |
| `TOPIC.yaml` → `verified_on` | **Demo machine** — where the verify **command actually ran** (vault `examples/` or external `code_repo`). **Never claim verified_on = runtime without re-running.** |
| `TOPIC.yaml` → `code_repo` | **Optional.** URL or path to runnable source when not in vault `examples/`. Evidence output still lives in vault `examples/README.md`. |

When they differ, practice / `examples/README.md` must:

1. State both explicitly (`runtime`, `verified_on`; link `code_repo` when set)  
2. Paste **real command output** (with date) for `verified_on`  
3. Note version deltas that affect output (e.g. JDK 8 `ExtClassLoader` vs 9+ `PlatformClassLoader`)

**External `code_repo`:** mastery-map **Command** uses that repo’s command (e.g. `mvn test -pl module`); after running, copy sample output into vault `examples/README.md`. Propagate the same command to topic README, practice, and main article — never leave obsolete vault-local commands.

## When to use `code_repo`

| Use vault `examples/` | Use external `code_repo` |
|-----------------------|---------------------------|
| Single script, snippet, or ≤2 small files | Multi-file demos, build tool (Maven/Gradle), tests, CI matrix |
| No separate repo worth maintaining | Org maintains a language/topic code monorepo |

Org may document a default layout (e.g. `java-research/<slug-module>/`) in vault `research/README.md` — skill stays repo-agnostic.

## Mastery-map template

```markdown
## Verification

- **Type:** demo | walkthrough | version-compare | repro
- **Command:** `…` (vault `examples/` or external `code_repo`)
- **Code:** `examples/` or `TOPIC.yaml` → `code_repo`
- **Verified on:** {JDK / env + date}
- **Narrative baseline:** {optional if same as TOPIC.runtime}
- **Expected / sample output:** → `examples/README.md` or inline fenced block
- **Walkthrough anchor:** `sources/…` or spec §
```

## Refresh triggers

Re-run Verify and update `verified_on` + sample output when:

- User asks (e.g.「在 JDK 8 上重跑 demo」或「重跑 code_repo 并同步 examples/README」）  
- Demo output shape changes (loader names, flags)  
- A new claim depends on runnable behavior

## Gate

- No fabricated output — run the command or mark demo **Open** in INDEX  
- Interview must not assert demo-only behavior that was not verified on `verified_on`
