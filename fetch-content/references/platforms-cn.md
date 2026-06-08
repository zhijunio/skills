# CN platforms — WeChat, Feishu, X

## 1. WeChat (`mp.weixin.qq.com`)

**Script path:** `fetch-url.sh` only (no separate script).

Order (opposite of generic web):

1. **`r.jina.ai`**
2. **`defuddle.md`**

**Why:** generic paywall rules false-positive on in-article “登录” ads. Script uses **`weixin_nonempty_ok`**: length ≥ 120 chars, no paywall scan on body.

**Limits:** anti-bot, login-only, deleted posts — not guaranteed.

**Fallbacks (in order):**

1. [`mcp-first.md`](mcp-first.md) (Firecrawl)
2. Browser copy / export PDF → [`platforms-github-pdf.md`](platforms-github-pdf.md) `pdf-extract.sh`
3. User paste

## 2. Feishu / Lark (`feishu.cn`, `larksuite.com`)

### Choose: this skill vs `lark-doc`

| User need | Use |
|-----------|-----|
| **Fetch Markdown for cite/archive/tech-mastery**; batch URL in research | **`fetch-content`** (`fetch_feishu.py` or script fallback) |
| **Edit, update, wiki v2, user OAuth**, comments, permissions | **`lark-doc`** skill + `lark-cli` (not this skill) |
| Link only, no creds, quick one-off | Try `fetch-url.sh`; if login wall → ask user for creds or switch to **`lark-doc`** |

### With app credentials (recommended for fetch-content skill)

```bash
export FEISHU_APP_ID=cli_xxx
export FEISHU_APP_SECRET=xxx
pip install -r "${FETCH_ROOT}/scripts/requirements-fetch.txt"   # requests

bash "${FETCH_ROOT}/scripts/fetch-url.sh" "https://xxx.feishu.cn/docx/TOKEN"
# or:
python3 "${FETCH_ROOT}/scripts/fetch_feishu.py" "https://..."
```

**App scopes:** `docx:document:readonly`, `wiki:wiki:readonly`

**Supported URLs:**

- `/docx/{token}` — direct export
- `/wiki/{token}` — resolves wiki node → docx when `obj_type` is docx

**Not supported:**

- Legacy **`/docs/`** — convert to docx in Feishu or open docx link
- Wiki nodes that are not docx (sheet, bitable, etc.) — export or use **`lark-doc`** / drive import

**Output:** `fetch_feishu.py` prepends YAML front matter (`title`, `document_id`, `url`); strip or keep when saving — see [`output-save-handoff.md`](output-save-handoff.md).

### Without credentials

`fetch-url.sh` falls through to generic defuddle → jina → curl (usually login-walled). Script prints a **stderr hint** suggesting `FEISHU_APP_*` or **`lark-doc`**.

## 3. X / Twitter

| Approach | Expectation |
|----------|-------------|
| `fetch-url.sh` | Often fails or returns empty shell |
| MCP / browser | May work when scripts do not |

**Never invent tweet/thread body text** on failure.

## 4. Platform quick table

| Host | Script order / tool | Cred needed |
|------|---------------------|-------------|
| WeChat | jina → defuddle | No |
| Feishu docx/wiki | `fetch_feishu.py` | App ID + secret |
| X | generic chain | No (usually insufficient) |
