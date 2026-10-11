---
name: sdd-review
description: >
  Two-axis Standards/Spec review of changes since a fixed point. Use when the user wants a branch, PR, or "review since X" quality report. Not whole-repo improve scan.
---

# sdd-review

Independent two-axis **quality report** of `HEAD` vs a fixed point (Matt Pocock `code-review` shape). Usable alone — not tied to Spec/Plan/Build. **No HTML**. Read-only.

- **Standards** — repo standards + shared baseline?
- **Spec** — originating issue / PRD / spec / plan (when one exists)?

Parallel sub-agents when available; aggregate findings + one-line summary + **Verdict**.

## When

Use for an incremental branch, PR, or fixed-point review. It checks the committed diff against repository Standards and an available Spec.

## Skip

Use [`sdd-improve`](../sdd-improve/SKILL.md) for a broad repo or module health scan. Use [`sdd-build`](../sdd-build/SKILL.md) to implement approved fixes after this report.

## Process

### 1. Pin the fixed point

User supplies commit / branch / tag / PR / `main` / `HEAD~5`, etc.

**If missing**, resolve the repo default branch — do **not** hard-code `main`:

1. `git symbolic-ref refs/remotes/origin/HEAD` → use that ref (e.g. `origin/master`)
2. Else `git rev-parse origin/main origin/master main master` — first that resolves
3. Else **ask** (offer any candidates you found)

Always record the **actual** ref used. In **Summary**, label it `fixed point: <ref> (default)` when step 1–2 chose it, or `fixed point: <ref> (user)` when the user supplied it.

Capture: `git diff <fixed-point>...HEAD`, `git log <fixed-point>..HEAD --oneline`, diff kind (`code` / `prose/docs-only`).

Confirm ref resolves and committed diff is non-empty — fail here, not in sub-agents. **Committed range only**; uncommitted WIP is out of scope.

Large diff: triage by subsystem/risk; note sampled/skipped on the Summary meta line.

### 2. Spec source

Order: user path → issue/PR in commits → `docs/` / `specs/` / `wiki/` / `.scratch/` → related repo docs.

Nothing found → ask once. No spec → skip Spec sub-agent (**Standards-only**); under `## Spec` write `no spec available` — do not claim Spec pass.

### 3. Standards sources

Repo docs (`AGENTS.md`, README, CONTRIBUTING, CODING_STANDARDS, CI, …) plus:

- Fowler smells under **Structure** — paste full [references/smell-baseline.md](references/smell-baseline.md) into the Standards sub-agent
- Four dimensions — [references/standards-baseline.md](references/standards-baseline.md): Correctness · Structure · Verification · Traceability
- Structural simplification lens in [references/standards-baseline.md](references/standards-baseline.md): code judo, spaghetti growth, direct boundaries, canonical ownership, signaled orchestration, and decomposition

Repo overrides baseline. Hard vs judgement per those files. Skip tooling-enforced style.

For Structure, actively ask whether the change adds concepts or merely rearranges complexity. Look for a code-judo simplification, new spaghetti growth, boundary indirection, misplaced ownership, avoidable orchestration, and unhealthy file/module growth. Use these as evidence-backed judgement calls, scoped to what the diff introduced or worsened; a missed ideal refactor is not a blocker by itself.

### 4. Spawn both axes

Parallel when possible; else sequential (note in Summary).

**Standards** — diff + commits + standards paths + **full** smell baseline + four dimensions. Present as Matt **(a)/(b)** with Delivery Group emoji on every finding (under 400 words):

- **(a) Documented standards & baseline dimensions** — cite repo standard file + rule when one applies; else cite [`standards-baseline.md`](references/standards-baseline.md) + dimension (Correctness / Structure / Verification / Traceability). Hard vs judgement; `file:line`; 🔴/🟡/🟢. Use this bucket for correctness/verification/traceability gaps that are **not** Fowler smells.
- **(b) Baseline smells** — name the Fowler smell; quote hunk; usually Structure; judgement only unless also breaking a documented rule or AC; smells default 🟢/🟡; 🔴 only with AC gap or hard rule; `file:line`

**Spec** — diff + commits + spec contents. Present as Matt **(a)/(b)/(c)** with Delivery Group emoji on every finding (under 400 words):

- **(a) Missing / partial** — quote `AC-n` / spec lines (including AC that requires proof but evidence is absent/weak)
- **(b) Scope creep** — behaviour in the diff not asked for
- **(c) Looks implemented but wrong** — quote spec; use recorded test/close-out evidence when present

**Bucket routing (do not duplicate across axes):**

| Situation | Put it in |
| --- | --- |
| Breaks `AGENTS.md` / CONTRIBUTING / CI-documented rule | Standards **(a)** · cite that file |
| Correctness / Verification / Traceability friction; no Fowler name | Standards **(a)** · dimension on `standards-baseline.md` |
| Named Fowler smell | Standards **(b)** |
| Test/proof gap vs repo test norms | Standards **(a)** · **Verification** first |
| Same gap also violates an `AC-n` / Constraints proof line | Spec **(a)** only if the AC/constraint is the contract; else keep Standards — **do not double-file** |
| Out of committed range (other repo, ops manual step) | Spec **(a)** partial *or* Summary meta note — not a fake Standards smell |
| Product taste / whole-repo debt not worsened by diff | Out of scope → [`sdd-improve`](../sdd-improve/SKILL.md) |

No spec → skip Spec sub-agent. Optional non-mutating commands only when they cut uncertainty; report evidence, not a review substitute.

### 5. Aggregate

Present under `## Standards` and `## Spec` separately — verbatim or lightly cleaned. **Do not merge, rerank, or pick one winner across axes.**

**Summary** (after both axes) — keep short:

```markdown
- Meta: fixed point: <ref> (user|default) · diff: code|prose · [sampled/skipped if any]
- Standards: <n> findings · worst: <one line or none>
- Spec: <n> findings · worst: <one line or none>   # or: Spec: Standards-only (no spec available)
```

**Verdict** (gate only, last): **blocked** (any 🔴) | **pass** (no 🔴, no unaccepted 🟡) | **pass pending risk acceptance** (🟡 until the user **explicitly accepts** the risk). Verdict must not rewrite or blend axis prose. When pending, Ask once: *Accept these 🟡 risks, or fix first?*

**Stop** after the report. Do not auto-chain fixes, git, PR, or merge. Wait for the user.

## Present

**Locale (hard rule):** Write the Present in the **user's language** for this conversation (findings, Summary prose, Ask, localized section titles). Do **not** default to English because skill templates are English. Keep untranslated: `AC-n`, skill ids, `file:line`, git refs, 🔴/🟡/🟢. Verdict tokens may stay as `blocked` / `pass` / `pass pending risk acceptance` with a short localized gloss, or fully localized if the user prefers.

Literals: `AC-n`, `file:line`, refs, skill ids, 🔴/🟡/🟢.

**Report Present** (Matt-shaped: axes first). Sections: **Standards** · **Spec** · **Summary** · **Verdict**.

### Standards body

```markdown
## Standards

### (a) Documented standards & baseline dimensions
- 🔴/🟡/🟢 **Title** · Dimension · hard|judgement — evidence · `path:line` — cite AGENTS/… or standards-baseline

### (b) Baseline smells
- 🔴/🟡/🟢 **Smell name** · Structure — quote hunk · `path:line`
```

Omit an empty subsection, or write `none` / 中文等价（如「无」）. Four dimensions label **(a)** findings; Fowler names live only under **(b)**. Do **not** replace (a)/(b) with dimension-first headings. Localize the title text after `(a)`/`(b)` (e.g. `(a) 文档标准与基线维度`).

### Spec body

```markdown
## Spec

### (a) Missing / partial
- 🔴/🟡/🟢 **Title** — quote `AC-n` / spec line — evidence

### (b) Scope creep
- 🔴/🟡/🟢 **Title** — what landed that was not asked for

### (c) Looks implemented but wrong
- 🔴/🟡/🟢 **Title** — quote spec — why the implementation mismatches
```

No spec → `## Spec` / 用户语言写「无规格可用」类文案（可用 `no spec available` 作机读旁注）. Omit empty (a)/(b)/(c) or write `none`/「无」。Localize titles after `(a)`/`(b)`/`(c)` (e.g. `(a) 缺失/部分实现`).

- Axes stay unmerged — never one blended findings list
- **Summary** — meta + one Standards line + one Spec line (template above)
- **Verdict** — gate only; pending → short Accept Ask

No standalone **Scope** / **Coverage**. No **Suggested next steps** — Stop is enough.

## Guidelines

| Group | When |
| --- | --- |
| **🔴 must-fix** | Blocks: correctness, security, AC/spec gap, data loss, non-goal break |
| **🟡 should-fix** | Fix or explicit accept: half migration, test gap, serious smell |
| **🟢 suggestion** | Non-blocking / mild smell |

Smells default 🟢/🟡; 🔴 only with AC/spec gap or documented hard rule. Not improve-pass priority labels.

| Request | Route |
| --- | --- |
| Branch / PR / since X / quality report | **This skill** (missing fixed point → default branch via `origin/HEAD`) |
| Repo / module / area improve pass | [`sdd-improve`](../sdd-improve/SKILL.md) |
| Over-engineering / bloat / what to delete only | [`ponytail-audit`](../ponytail-audit/SKILL.md) |
| Push / PR / merge / tag | Out of pack |

Do not: edit files; review WIP by default; treat pre-existing whole-repo debt as must-fix unless this diff worsened it; claim Spec pass without a spec; pick one winner across axes; auto-start follow-up work after the report.

## Why two axes

Standards pass + Spec fail (right style, wrong thing) vs Spec pass + Standards fail (right thing, wrong conventions). Keep axes separate so one cannot mask the other.
