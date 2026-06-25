# CN platforms — WeChat, Feishu, X

## WeChat (`mp.weixin.qq.com`)

`read.sh` order: **jina → defuddle** (inverse of generic web).

Uses `weixin_nonempty_ok` (≥ 120 chars) — no paywall scan on in-body「登录」ads.

Fallbacks: `mcp-fallback.md` → PDF export → user paste.

## Feishu / Lark (`feishu.cn`, `larksuite.com`)

| Need | Approach |
|------|----------|
| Markdown for cite / archive / topic sources | `feishu-fetch.py` + app creds |
| Edit, wiki OAuth, permissions | External lark-doc flow (not this skill) |
| No creds | `read.sh` generic chain (usually fails) |

```bash
export FEISHU_APP_ID=cli_xxx FEISHU_APP_SECRET=xxx
pip install -r "${READ_ROOT}/scripts/requirements.txt"
bash "${READ_ROOT}/scripts/read.sh" "https://xxx.feishu.cn/docx/TOKEN"
```

Scopes: `docx:document:readonly`, `wiki:wiki:readonly`.  
Supports `/docx/`, `/wiki/` (docx nodes). Legacy `/docs/` unsupported.

Output may include YAML front matter from `feishu-fetch.py` — see `output.md`.

## X / Twitter

`read.sh` often insufficient → `mcp-fallback.md`. Never invent thread text.

## Quick table

| Host | Tool | Creds |
|------|------|-------|
| WeChat | read.sh jina→defuddle | No |
| Feishu | feishu-fetch.py | App ID + secret |
| X | MCP after read.sh fail | — |
