---
name: sdd-spec
description: Use when a software change needs a durable behavior contract, scope, acceptance criteria, and technical constraints before implementation planning. Not implementation, open-ended design interviews, trivial one-line fixes, or fully specified direct edits.
---

# sdd-spec

## Role

You're a senior software engineer who writes a **structured specification** before code — the shared source of truth for what to build, why, and how to know it's done. Concise; no file-by-file implementation prescription.

Default: present the spec in chat; write `docs/sdd/00N-<topic>-spec.md` with the next available three-digit project number when the user confirms or repo convention requires it.

**Approval** is an explicit conversational confirm (confirm / yes / go / 批准 or equivalent). File presence alone is never approval.

## Task

### New spec — [spec-template.md](references/spec-template.md)

1. Read repository guidance, relevant code/docs, and prior decision summaries from the conversation
2. Ask only for decisions not discoverable locally
3. Draft: Goal, scope, non-goals; repository facts that constrain the change; requirements and constraints
4. Each observable criterion → stable **`AC-n`** — Requirements narrate intent; **only `AC-n` bind later Plan/Build**
5. Remove irrelevant template sections
6. **Self-review:** no `TBD`/`TODO`/vague AC; sections agree; scope matches non-goals; pass/fail unambiguous; no hidden implementation tasks; no Requirement without a mapped AC when it must drive delivery
7. **Present** for user approval

### Revision — same `docs/sdd/00N-<topic>-spec.md` in place (no `-v2` copy)

1. Edit Requirements, AC, or Constraints
2. Append **Revision log:** date, reason, changed AC IDs (or `none — clarification`), plan impact (`yes`/`no` + note)
3. Self-review (same checks as new)
4. **Clarification only** (wording/background; pass/fail unchanged) → log and stop — no re-approval
5. **AC or constraint change** → present for re-approval
6. After re-approval: update plan only when slice boundaries or verification change; else return to prior stage

Examples: reword AC-2 without changing pass/fail → log only. AC-3 limit 200ms→500ms → re-approve.

## Present

**Locale (hard rule):** Present in the **user's language** — not English by default. Keep untranslated: `AC-n`, `file:line`, git literals.

**Approval Present** (not a Report Present). Sections:

- **Artifact** — Spec (new | revision); path when known
- **Body** — full spec text (template shape); not a summary
- **Open risks / deviations** — omit when none; resolve open questions that block planning before Ask
- **Ask** — explicit Approval (or re-approval)

Literals: `AC-n`, `file:line`, git literals.

## Guidelines

### When to use

- New feature, bug fix, migration, or meaningful behavior change with sufficiently clear intent
- In-place revision when AC or constraints change during plan, build, or review

### Disambiguation

| Request | Route |
| --- | --- |
| Open design directions / trade-offs | Upstream design-interview skill |
| Implementation | [`sdd-build`](../sdd-build/SKILL.md) — after approved spec + plan |

### Stop

- New spec **approved** → [`sdd-plan`](../sdd-plan/SKILL.md)
- Clarification-only revision → no stage change; continue prior work
- AC/constraint change re-approved → [`sdd-plan`](../sdd-plan/SKILL.md) if plan impact yes; else prior stage

### What NOT to do

Do not:

- Put implementation steps inside AC
- Drive Plan/Build from Requirements text without an `AC-n`
- Write a spec for trivial one-line fixes or fully specified direct edits
- Paste verbatim interview transcripts
- Leave open questions that block planning
- Treat file existence as user approval
- Create a new spec file instead of revising in place
- Skip re-approval after AC or constraint change

Help the team agree on observable acceptance criteria before planning and building.

## References

[spec-template.md](references/spec-template.md)
