# Fetch chain — scripts and paywalls

## 1. One-shot entry

```bash
bash "${FETCH_ROOT}/scripts/fetch-url.sh" "<url-or-local-pdf-path>"
```

`fetch-url.sh` branches by URL type (GitHub, PDF, Feishu, WeChat, generic). See other references for platform specifics.

## 2. Generic web order

Inside `fetch-url.sh` for ordinary http(s) pages:

1. **`defuddle.md`** — `https://defuddle.md/{url}`
2. **`r.jina.ai`** — `https://r.jina.ai/{url}`
3. **`curl`** — direct fetch with browser-like User-Agent

Each hop must pass **`nonempty_ok`**: body ≥ 60 chars and first ~30 lines must not match paywall hints.

## 3. Paywall heuristics

If early body matches (case-insensitive, EN + ZH), that hop is **rejected** and the next method runs:

- subscribe, sign in, 登录, 继续阅读, 付费内容, 此内容已被发布者删除

**Rules:**

- All hops fail → exit 1; report honestly (list methods tried on stderr).
- **Do not save login walls as content.**
- User still wants a stub file → HTML comment `<!-- likely paywall -->` at top only.

WeChat uses a separate length check (`weixin_nonempty_ok`); see [`platforms-cn.md`](platforms-cn.md).

## 4. Local PDF only

```bash
bash "${FETCH_ROOT}/scripts/pdf-extract.sh" "/path/to/file.pdf"
```

Dependencies (either):

- **poppler**: `pdftotext` (e.g. `brew install poppler`)
- **Python**: `pip install -r "${FETCH_ROOT}/scripts/requirements-fetch.txt"` (`pypdf`)

## 5. When scripts are not enough

| Signal | Next step |
|--------|-----------|
| Empty body from all hops | [`mcp-first.md`](mcp-first.md) |
| Page is mostly client-rendered | MCP before re-running scripts |
| User pasted cookie/session | Browser MCP or manual paste — do not fake body text |

## 6. Anti-patterns

| Do not | Do instead |
|--------|------------|
| Bare `curl` for tech-mastery Source when `fetch-content` exists | `fetch-url.sh` (paywall + CN platforms) |
| Invent article text when fetch failed | Report failure; offer MCP or paste |
| Summarize before fetch when user said "read this URL" | Fetch first; summarize only if asked after |
