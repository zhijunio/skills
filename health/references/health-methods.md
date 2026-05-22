# Health methods reference (this repo)

When auditing the **AI collaboration stack** (project docs → rules → skills → hooks/automation → tools/MCP → secrets), read this file first, then run **summary** or **deep** mode. Boundaries and report shape follow parent **`health/SKILL.md`**.

Designed **multi-platform**: detect host from **marker files + user declaration**; do not assume Cursor-only.

## 1. Summary vs deep

| Mode | When | What |
|------|------|------|
| **Summary** (default) | User did not ask thorough/complete/deep | `collect-context.sh`, **`## agent_markers`**, sample 1–3 rule entry files, list MCP, findings |
| **Deep** | User asks, or summary finds `[!]` / control plane unclear | Full rules/hooks, per-MCP harmless probes, product debug per current docs; warn about cost first |

Do not apply **Complex** criteria to **Simple** repos (`SKILL.md` Step 0).

## 2. Identify host (required)

State in the report **opening**:

1. **User declaration**: primary agent(s) (e.g. Cursor + Copilot).
2. **Repo markers**: platform table below + `collect-context.sh` **`## agent_markers`** (`yes` on many rows ⇒ multiple hosts may be active).
3. **Conclusion**: `primary=…`, `also_present=…`, or `ambiguous → asked user`.

If **`~/.codex` / `~/.continue`** is unreadable: finding **「need user to paste redacted tree or config」** — do not guess.

### 2.1 Platform marker table (`$ROOT`)

Path **exists** ⇒ platform may be in use (verify against each product's current docs).

| Platform | Common repo markers | User-level (if audit env can read) |
|----------|---------------------|-------------------------------------|
| **Cursor** | `.cursor/rules/`, `.cursor/mcp.json`, `.cursor/hooks.json` | Cursor app settings |
| **Claude Code** | `CLAUDE.md`, `.claude/rules/`, `.claude/settings.local.json` | `~/.claude/` |
| **Codex CLI** | `AGENTS.md` (repo) | `~/.codex/AGENTS.md` |
| **GitHub Copilot** | `.github/copilot-instructions.md`, `AGENTS.md`, `.github/instructions/` | Editor Copilot settings |
| **Continue** | `.continue/` (if present), may share `AGENTS.md` | `~/.continue/config.json` |
| **Windsurf** | `.windsurfrules` | Cascade / app rules |
| **Cline / Roo** | `.clinerules`, `.roomodes` | Extension settings |
| **Amazon Q** | `.amazonq/rules/` | Q config |
| **Aider** | `.aider.conf.yml`, `CONVENTIONS.md` | `~/.aider.conf.yml` |

**No markers and no user declaration**: `primary=unknown`; run **§4 generic** only and ask which agent they use.

## 3. One-shot collection (recommended first step)

```bash
bash /path/to/zhijunio-skills/health/scripts/collect-context.sh "$ROOT"
```

Self-check in this repo:

```bash
bash health/scripts/collect-context.sh "$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
```

Output includes **`## agent_markers`**, **`## paths_present`**, **`skill_md_index`**. Read-only; do not `cat` secret file bodies.

## 4. Repo-generic checks (host-agnostic)

### 4.1 Entry docs

- [ ] `README.md` exists and is non-empty (Simple: how to run/test).
- [ ] `AGENTS.md` or `CLAUDE.md` (Standard+: one recommended; multi-agent teams may document each product).

### 4.2 Git hygiene (paths only)

```bash
git -C "$ROOT" status -sb 2>/dev/null || true
git -C "$ROOT" ls-files 2>/dev/null | grep -Ei '(^|/)\.env($|[.])|(^|/)id_(rsa|ed25519)|(^|/)\.pem$|settings\.local\.json|(^|/)\.npmrc$|(^|/)\.pypirc$' || true
```

Tracked `.env` / private keys / `settings.local.json` → usually `[!]`.

### 4.3 CI / size

```bash
test -d "$ROOT/.github/workflows" && echo has_ci=yes || echo has_ci=no
```

## 5. Cursor (when markers present)

| Check | How |
|-------|-----|
| Rules | `ls` `.cursor/rules`; duplication/conflict with `AGENTS.md` / `CLAUDE.md` |
| MCP | `.cursor/mcp.json` valid JSON; respect `enabled: false` |
| Hooks | `hooks.json` script paths; remote unverified scripts |

## 6. Claude Code

| Check | How |
|-------|-----|
| Rules | `.claude/rules/` vs `CLAUDE.md` |
| Local override | `.claude/settings.local.json` committed by mistake |
| Deep | Compare team `collect-data.sh` (if any) to this skill's summary; facts baseline = `collect-context.sh` |

## 7. Codex CLI

- Repo `AGENTS.md` vs `~/.codex/AGENTS.md`: duplicate or conflicting blocks.

## 8. GitHub Copilot / editor AI

- `.github/copilot-instructions.md`, `.github/instructions/` vs `AGENTS.md` / `.cursor/rules` triplicated → `[~]` merge to single source of truth.
- Instructions with realistic-looking secrets in examples → `[!]`.

## 9. Continue

- Project `.continue/` or readable `~/.continue/`: MCP/model vs repo rules; never print API keys.

## 10. Windsurf

- `.windsurfrules` size/sections; conflict with `AGENTS.md`.

## 11. Cline / Roo

- `.clinerules` / `.roomodes` vs Cursor rules or `AGENTS.md`.

## 12. Amazon Q

- `.amazonq/rules/` vs other rule entry points.

## 13. Aider

- `.aider.conf.yml` secrets in repo; `CONVENTIONS.md` vs `AGENTS.md`.

## 14. MCP probes (any platform with MCP)

Harmless probe per server where `enabled != false`; no full keys in report.

## 15. Hooks

Before blaming config: product debug logs / UI blocking hook delivery.

## 16. Skills repo (e.g. `zhijunio-skills`)

Each `*/SKILL.md` `name` / `description`; chained skills (`read → learn → humanize / edit-article`) point at correct repo paths.

## 17. Finding format

`Action:` must be copy-pasteable; else **repro steps + official doc link**.

## 18. Out of scope

Business logic review; automatic file edits.

## 19. Maintenance

Platform path table maintained here; product behavior follows **official current documentation**.
