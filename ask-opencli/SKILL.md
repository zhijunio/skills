---
name: ask-opencli
description: Ask Grok or Gemini via opencli (browser-session based, no API cost). Use when the user says "问 grok", "问 gemini", "ask grok", "ask gemini", "用 grok 查", "grok 怎么看", "让 gemini 分析", or wants a second opinion from Grok/Gemini without paying API tokens. Routes to opencli which drives the already-logged-in Chrome session.
argument-hint: "Question or task to send to Grok/Gemini"
---

# Ask via OpenCLI (Grok / Gemini)

Use `opencli` to query Grok or Gemini through the user's already-logged-in Chrome session. Zero API cost, fully driven by the existing browser session.

> **opencli 项目**：https://github.com/jackwener/opencli
> **安装**：`npm install -g @jackwener/opencli`（需要 Node ≥20）
> **首次运行**：`opencli doctor` 会自动引导安装 Chrome Browser Bridge 扩展
> **登录**：用装了扩展的 Chrome profile 打开 grok.com 和 gemini.google.com 登录一次
> **必须环境变量**：`export OPENCLI_BROWSER_COMMAND_TIMEOUT=300`（写进 shell rc）


## Model selection

If the user doesn't specify which model, default to **Grok** for real-time / social / news / tech-trend questions, and **Gemini** for long-form reasoning, research, coding, and document analysis.

If still ambiguous, ask the user which one to use before sending.

## Output format (default: JSON)

Use **`-f json`** (Grok) or **`--format json`** (Gemini when supported) for every agent-driven call so responses are parseable and artifacts stay structured.

Use **`--format plain`** only when the user explicitly wants human-readable terminal output and no artifact parsing is needed.

## ⚠️ 常见错误命令（直接 fail fast）

| ❌ 错误 | ✅ 正确 | 原因 |
|---|---|---|
| `opencli gemini chat "..."` | `opencli gemini ask "..."` | 没有 `chat` 子命令 |
| `opencli grok chat "..."` | `opencli grok ask "..."` | 只有 `ask` 子命令 |
| `opencli grok new` | `opencli grok ask "..." --new true` | grok 的"新对话"是 `ask` 的参数 |
| `opencli gemini query "..."` | `opencli gemini ask "..."` | 没有 `query` 子命令 |

**完整子命令速查**（运行 `opencli grok --help` 或 `opencli gemini --help` 查看）：

```
Grok:
  ask <prompt>                    # 唯一命令

Gemini:
  ask <prompt>                    # 最常用
  new                             # 开新对话
  deep-research <prompt>          # Deep Research 模式
  deep-research-result [query]    # 取 Deep Research 结果
  image <prompt>                  # 图片生成
```

## ⚠️ Critical: Two-layer timeout

opencli has **two independent timeout layers**. For Grok (and long Gemini answers), set both:

1. **Inner**: `--timeout 300` — site response wait time
2. **Outer**: `OPENCLI_BROWSER_COMMAND_TIMEOUT=300` — entire browser command timeout (**default 60s** in opencli `runtime.js`)

The outer limit wins. Setting only `--timeout 300` still gets killed at 60s.

**Recommended**: add `export OPENCLI_BROWSER_COMMAND_TIMEOUT=300` to `~/.zshrc`.

## Commands

### Grok (MANDATORY env var for long answers)

```bash
OPENCLI_BROWSER_COMMAND_TIMEOUT=300 opencli grok ask "{{PROMPT}}" --timeout 300 -f json
```

Or if env var already exported in shell:

```bash
opencli grok ask "{{PROMPT}}" --timeout 300 -f json
```

Flags:
- `--new true` — start a fresh chat (avoids polluting prior conversation context)
- `--web true` — use the hardened grok.com consumer web flow (better error messages)
- `--timeout 300` — MUST match or be lower than `OPENCLI_BROWSER_COMMAND_TIMEOUT`
- `-f json` — default for agent calls; use `-f plain` only when user asks for plain text

### Gemini

```bash
OPENCLI_BROWSER_COMMAND_TIMEOUT=300 opencli gemini ask "{{PROMPT}}" --timeout 90 --format json
```

Gemini is usually more stable than Grok. Still set the outer timeout for long answers.

Flags:
- `--new true` — start a fresh chat
- `--timeout 90` — extend timeout for long answers (raise with outer env var if needed)
- `--format json` — default for agent calls

If `--format json` is unsupported in the installed opencli version, fall back to `--format plain`, note the version in the artifact, and parse text manually.

### Parallel dispatch (for multi-AI workflows)

To call grok and gemini in parallel, use Bash with `run_in_background=true` for both calls in the same assistant turn.

For deep cross-validation (N internal sub-agents + 2 external AIs, confidence-tiered synthesis), use **多源并行调研与交叉验证** when that skill is installed; otherwise orchestrate manually with parallel opencli 浏览器会话查询 calls and a synthesis step.

## Prerequisites (check before running)

1. **opencli installed**: `opencli --version` should print a version.
2. **Chrome running with Browser Bridge extension loaded** — run `opencli doctor`; it guides extension install/load. Do not assume a fixed path on disk.
3. **Logged in to the target site**:
   - Grok: `grok.com` must be logged in
   - Gemini: `gemini.google.com` must be logged in
4. **Daemon connectivity**: run `opencli doctor` once per session if unsure.

If any check fails, **do not fall back to another tool silently**. Report the missing prerequisite and ask the user to fix it.

## Execution protocol

1. Decide which model to use (Grok vs Gemini). If unclear, ask.
2. Build the prompt. Be explicit — these browser UIs have no system prompt so the full question must be self-contained.
3. Run the opencli command with **`-f json` / `--format json`** (or plain only if user requested).
4. Capture the response. If the command fails with a session/auth/challenge message, stop and surface the error to the user. Do NOT retry blindly.
5. Save an artifact to the **OS temp directory** (not the workspace) — see below.
6. Report back to the user: model used, summary of response, and **full path** to the artifact.

## Error handling

opencli will emit specific errors like:
- "Not logged in to grok.com" → ask user to log in
- "Composer not found" → DOM changed, ask user to open the site manually and retry
- "Session gated / challenge" → user needs to solve the challenge in Chrome manually
- Timeout → offer to retry with a longer `--timeout` and matching `OPENCLI_BROWSER_COMMAND_TIMEOUT`

Do not swallow these. Report verbatim with the fix action.

## Artifact requirement

After every successful call, save a markdown artifact **outside the git workspace** — same convention as the 会话交接文档 skill.

Path pattern:

```text
${TMPDIR:-/tmp}/ask-opencli-<grok|gemini>-<slug>-<YYYYMMDD-HHMMSS>.md
```

Create the file explicitly, for example:

```bash
ARTIFACT="${TMPDIR:-/tmp}/ask-opencli-grok-my-topic-$(date +%Y%m%d-%H%M%S).md"
```

Minimum sections:
1. **Original user task** — what the user asked you
2. **Model** — grok or gemini, plus any flags used (--new, --web, --timeout, format)
3. **Final prompt sent** — the exact string passed to `opencli ... ask`
4. **Raw response** — the full JSON or plain output from opencli
5. **Summary** — 2-3 bullets distilling the key points
6. **Action items / next steps** — what to do with this information

Keep the artifact even when the response is low quality — it documents that the query was made.

Optional: if the user uses an `.omx/artifacts/` workflow, copy or symlink there **after** writing to the OS temp path; do not require `.omx/` to exist.

## Examples

```bash
# Quick factual question, Grok
OPENCLI_BROWSER_COMMAND_TIMEOUT=300 opencli grok ask "最新的 Claude 定价和上一代有什么差别？" --timeout 120 -f json

# Long reasoning task, Gemini with extended timeout + fresh chat
OPENCLI_BROWSER_COMMAND_TIMEOUT=300 opencli gemini ask "分析这段 Rust 代码的并发安全问题：..." --new true --timeout 120 --format json

# Grok hardened web path with new chat
OPENCLI_BROWSER_COMMAND_TIMEOUT=300 opencli grok ask "X 上这两天关于 agent framework 的讨论焦点是什么？" --web true --new true --timeout 300 -f json
```

Task: {{ARGUMENTS}}
