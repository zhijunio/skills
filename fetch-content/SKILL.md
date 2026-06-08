---
name: fetch-content
description: Turn URLs or PDFs into clean Markdown for citation, archive, or downstream tech-mastery/humanize. Use when the user gives a link, wants a page read, article fetch, or PDF text. Load references/fetch-methods.md before fetching. Prefer scripts in this skill directory; not for plain text files already in the repo (use the Read tool).
---

# Fetch content — URL / PDF → Markdown

Self-contained under **`fetch-content/scripts/`** + **`fetch-content/references/`**. Paths and commands are relative to this repo.

You may inline `🥷` at the start of the first reply.

**Routing, platform notes, MCP pairing, and tech-mastery handoff** live in [`references/`](references/); this page keeps boundaries and quick routing.

## Before running

1. Open **[`references/fetch-methods.md`](references/fetch-methods.md)** — use the **decision tree** and load references for the URL type.
2. Default command:

```bash
bash fetch-content/scripts/fetch-url.sh "https://…"
```

`FETCH_ROOT` = `fetch-content/` (relative) or absolute `.../zhijunio-skills/fetch-content`.

## Boundaries

- **Default: convert only** — no summary or interpretation unless the user asks **after** fetch.
- **Non-PDF text already in the repo**: use the editor **Read** tool, not this skill.
- **Feishu edit / wiki v2 / user OAuth**: use **`lark-doc`**, not this skill — see [`references/platforms-cn.md`](references/platforms-cn.md).

## Routing summary

| Scenario | Reference / approach |
|----------|----------------------|
| Pick method, paywalls, generic order | [`fetch-chain.md`](references/fetch-chain.md) |
| WeChat / Feishu / X | [`platforms-cn.md`](references/platforms-cn.md) |
| GitHub / PDF | [`platforms-github-pdf.md`](references/platforms-github-pdf.md) |
| Heavy JS / anti-bot | [`mcp-first.md`](references/mcp-first.md) before or after script failure |
| Save, output shape, **tech-mastery** | [`output-save-handoff.md`](references/output-save-handoff.md) |
| **tech-mastery** Source (JEP, OpenJDK raw) | [`tech-mastery-sources.md`](references/tech-mastery-sources.md) |

| Scenario | Script |
|----------|--------|
| Generic URL | `fetch-url.sh` (defuddle → jina → curl) |
| WeChat | `fetch-url.sh` (**jina → defuddle**) |
| Feishu (app creds) | `fetch_feishu.py` via `fetch-url.sh` |
| GitHub blob | raw + optional `gh` |
| Local/remote PDF | `pdf-extract.sh` / PDF branch in `fetch-url.sh` |

## Output and save

Follow [`references/output-save-handoff.md`](references/output-save-handoff.md):

- **Default**: Markdown in chat only; **no new file**.
- **Save / tech-mastery Source**: pipe to `save-md.sh`.
- **Images**: only when user asks.

## Gotchas

| Situation | Handling |
|-----------|----------|
| “Read this” but they want a summary | Markdown first; summarize after |
| Response is JSON | Extract body field |
| Content too long | Truncate with note, or save full file |
| Fetch failed | Report honestly; MCP or paste — never invent body |

## Pairing with `tech-mastery`

Multi-source technical topics: one fetch per URL via this skill; filing and chain → [`output-save-handoff.md`](references/output-save-handoff.md) §2–§6.

## References

| File | Contents |
|------|----------|
| [`fetch-methods.md`](references/fetch-methods.md) | Router + decision tree |
| [`fetch-chain.md`](references/fetch-chain.md) | Script order, paywalls |
| [`platforms-cn.md`](references/platforms-cn.md) | WeChat, Feishu, fetch-content vs lark-doc |
| [`platforms-github-pdf.md`](references/platforms-github-pdf.md) | GitHub, PDF deps |
| [`mcp-first.md`](references/mcp-first.md) | Firecrawl / MCP before scripts |
| [`output-save-handoff.md`](references/output-save-handoff.md) | Wrapper, save, tech-mastery, troubleshooting |
| [`tech-mastery-sources.md`](references/tech-mastery-sources.md) | JEP, OpenJDK raw, tech-mastery Source |
