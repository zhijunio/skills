# Fetch-content reference router (this repo)

Read this before fetching. Script root:

`FETCH_ROOT` = this repo's `fetch-content/` directory (contains `scripts/`, `references/`).

## 1. Loading router

| Scenario | Read first |
|----------|------------|
| Pick script vs MCP, paywall chain, generic web order | [`fetch-chain.md`](fetch-chain.md) |
| WeChat, Feishu/Lark, X; **fetch-content vs lark-doc** | [`platforms-cn.md`](platforms-cn.md) |
| GitHub raw/blob, local/remote PDF | [`platforms-github-pdf.md`](platforms-github-pdf.md) |
| Heavy JS, anti-bot, Firecrawl | [`mcp-first.md`](mcp-first.md) |
| **主题 mastery 研究** Source — JEP, OpenJDK raw, 404 handling | [`topic-sources.md`](topic-sources.md) |
| Output wrapper, save, images, **主题 mastery 研究** handoff, troubleshooting | [`output-save-handoff.md`](output-save-handoff.md) |

**Quick default** (http(s) or local `.pdf`):

```bash
bash "${FETCH_ROOT}/scripts/fetch-url.sh" "https://example.com/article"
```

Success → stdout body (often Markdown). Failure → stderr + non-zero exit. **Quote URLs** containing `&`.

## 2. Decision tree (agent)

```
URL or local .pdf?
├─ File already in repo (not PDF fetch) → editor Read tool, NOT this skill
├─ Heavy JS / known anti-bot → mcp-first.md, then scripts if MCP fails
├─ feishu.cn / larksuite.com → platforms-cn.md (API creds vs lark-doc)
├─ mp.weixin.qq.com → platforms-cn.md (jina → defuddle in script)
├─ github.com / raw.githubusercontent.com → platforms-github-pdf.md
├─ .pdf or Content-Type pdf → platforms-github-pdf.md
└─ else → fetch-chain.md (defuddle → jina → curl)
```

## 3. Relationship to parent `SKILL.md`

- **Boundaries, default output contract** → [`SKILL.md`](../SKILL.md).
- This directory adds **routing, platform notes, MCP pairing, topic Source** so `SKILL.md` stays short.

## 4. Out of scope (do not auto-start)

| Need | Action |
|------|--------|
| Multi-source technical topic writing | User runs a separate research workflow; save fetches under `<topic-dir>/sources/` when they provide a topic dir |
| De-AI / polish existing draft | Not part of fetch — user requests separately |
| Feishu doc edit/fetch v2 with user OAuth | **`lark-doc`** tool flow when configured — see [`platforms-cn.md`](platforms-cn.md) |
| MCP fetch when scripts fail | Firecrawl MCP when installed — see [`mcp-first.md`](mcp-first.md) |
