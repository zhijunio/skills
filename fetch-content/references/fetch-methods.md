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
| **tech-mastery** Source — JEP, OpenJDK raw, 404 handling | [`tech-mastery-sources.md`](tech-mastery-sources.md) |
| Output wrapper, save, images, **tech-mastery** handoff, troubleshooting | [`output-save-handoff.md`](output-save-handoff.md) |

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

- **Boundaries, default output contract** → **`fetch-content/SKILL.md`**.
- This directory adds **routing, platform notes, MCP pairing, tech-mastery Source** so `SKILL.md` stays short.

## 4. Sibling skills

| Skill | When |
|-------|------|
| **`tech-mastery`** | Multi-source technical mastery; Source fetch per URL via this skill |
| **`humanize`** | After user has text; not part of fetch |
| **`lark-doc`** | Feishu doc **edit/fetch v2** when user identity + lark-cli already configured (see `platforms-cn.md`) |
| **Firecrawl skill** | MCP fetch when scripts fail or page is JS-heavy |
