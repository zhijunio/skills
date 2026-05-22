---
name: health
description: Audit the AI collaboration stack (rules, MCP/tools, hooks, secrets, repo hygiene). Multi-platform: detect host from marker files and user declaration. Covers Cursor, Claude Code, Codex CLI, GitHub Copilot, Continue, Windsurf, Cline/Roo, Amazon Q, Aider (see references/health-methods.md). Default summary audit; can run health/scripts/collect-context.sh. Not for app bugs or PR code review.
---

# Health — collaboration stack (multi-platform)

**Path matrix, host detection, commands** → **[`references/health-methods.md`](references/health-methods.md)**. **`scripts/collect-context.sh`** prints which platform marker files exist.

You may inline `🥷` at the start of the first reply.

## Before running

1. Open **[`health/references/health-methods.md`](references/health-methods.md)**: **§2 identify host**, then check the matching platform section from **§5 onward**.
2. At repo root: **`bash health/scripts/collect-context.sh "$(git rev-parse --show-toplevel)"`** — paste **`## agent_markers`** and **`## paths_present`** into report **Facts**; **do not** `cat` sensitive paths.

## Multi-platform principles

- **Same six-layer model** (project docs → rules → skills/playbooks → hooks/automation → tools/MCP → secrets) for every host; **paths differ**.
- **Coexistence**: `.cursor/`, `CLAUDE.md`, `.windsurfrules`, etc. may all exist — list **what applies** (ask which agent the user uses); flag **duplication or conflict** as `[~]`.
- **Unknown host**: repo-generic checks + `agent_markers` only; host-specific layers `n/a`, no penalty.

Teams using **Claude Code** with their own health collectors (e.g. `collect-data.sh`) may compare against this skill; this repo does not ship those scripts.

### Six layers (quick → details in health-methods)

| Layer | Examples |
|-------|----------|
| Project entry | `README.md`, `AGENTS.md`, `CLAUDE.md` |
| Behavior rules | `.cursor/rules/`, `.claude/rules/`, `.windsurfrules`, `.clinerules`, `.github/copilot-instructions.md`, `.amazonq/rules/` |
| Skills / playbooks | `*/SKILL.md`, Claude `skills/`, product project docs |
| Hooks / automation | `.cursor/hooks.json`, IDE tasks (per product) |
| MCP / tools | `.cursor/mcp.json`, Continue config, Claude `settings.local.json` |
| Secrets / local overrides | `.env`, `*.pem`, `settings.local` — must not be committed |

## Boundaries

- **Stack configuration**, not business logic correctness.
- **No auto-edits**; `Action:` = copy-paste commands or explicit edit steps after user confirmation.

## Output language

Follow project conventions (`AGENTS.md`, `CLAUDE.md`, Copilot instructions, platform rules); otherwise match the user's language.

## Budget and depth

- **Default summary**: `collect-context.sh` + sample key files + MCP list and harmless probes if possible.
- **Deep audit**: user asks thorough/complete, or `[!]` found — expand first; warn about time and tokens.

## Step 0: project tier (calibrate expectations)

| Tier | Signals | Expectations (relax for small repos) |
|------|---------|--------------------------------------|
| **Simple** | Small repo, no CI | README or AGENTS; 0–1 rule files OK |
| **Standard** | CI or small team | README + repo rules; clear MCP/tool list |
| **Complex** | Many packages, MCPs, hooks | Layered rules, discoverable skills, least privilege, no secrets in git |

**Do not grade Simple projects with Complex criteria.**

## Steps 1–4 (details in health-methods)

- **Step 1 Facts**: generic repo + per-host; prefer **`collect-context.sh`** output.
- **Step 2 MCP**: harmless probes; skip `enabled: false`; never paste full keys in the report.
- **Step 3 Rules & skills**: conflicts, duplication, over-broad globals, `name`/`description` quality on `*/SKILL.md`.
- **Step 4 Report**: template below.

```text
Health Report: {project} ({Simple|Standard|Complex}, host: …)

### Facts (from collect-context or equivalent)

### [PASS] (table, ≤5 rows)

### Finding
- [severity] symptom (file:line if known)
  Why: one line
  Action: copy-paste command or concrete edit
```

**Severity**: `[!]` secrets / dangerous allowedTools / MCP abuse; `[~]` layering and duplication; `[-]` hygiene and docs.

If clean: `All relevant checks passed. Nothing to fix.`

## Gotchas

| Situation | Handling |
|-----------|----------|
| Missing `python3`/`jq` | `collect-context.sh` notes `json_skip`; not a defect unless the project needs JSON CI |
| Hook "not firing" | Check host debug / UI overlay before blaming config |
| External health scripts | Use this repo's `collect-context.sh` as facts baseline; note conflicts side by side |
