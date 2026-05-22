# Read methods reference (this repo)

Read this before fetching. Script root:

`READ_ROOT` = this repo's `read/` directory (contains `scripts/`, `references/`).

## 1. One-shot fetch (recommended)

For **http(s) URL** or local **`.pdf`** path:

```bash
bash "${READ_ROOT}/scripts/read-url.sh" "https://example.com/article"
```

Success: stdout is body (often Markdown). Failure: stderr message, non-zero exit.

**GitHub**: `raw.githubusercontent.com` direct; `github.com/.../blob/...` → **raw**; on 404 with `gh` auth, try **private raw** (`Accept: application/vnd.github.raw`).

**Remote PDF**: try `r.jina.ai`; on failure download temp file then `pdf-extract.sh`.

**General web**: `defuddle.md` → `r.jina.ai` → `curl` (order inside `read-url.sh`).

## 2. Local PDF only

```bash
bash "${READ_ROOT}/scripts/pdf-extract.sh" "/path/to/file.pdf"
```

Dependencies (either):

- **poppler**: `pdftotext` (e.g. `brew install poppler`)
- **Python**: `pip install -r "${READ_ROOT}/scripts/requirements-read.txt"` (`pypdf`)

## 3. Save to ~/Downloads

Pipe Markdown to file (pairs with `learn` Phase 1):

```bash
bash "${READ_ROOT}/scripts/read-url.sh" "https://example.com/page" \
  | bash "${READ_ROOT}/scripts/save-md.sh" "example-page"
```

`save-md.sh` takes a **filename slug** (alphanumeric `._-` only; others become `_`). Existing files get `-1`, `-2` suffixes; no overwrite.

Script prints the **final absolute path** (one line).

## 4. Proxy chain and paywalls

- If early body lines match Subscribe / Sign in / login-to-read / paid content / deleted post, `read-url.sh` fails that hop and tries the next; all fail ⇒ exit 1.
- **Do not save login walls as content**; if the user still wants a stub file, HTML-comment «likely paywall» at top.

Paywall heuristics include **English and Chinese** phrases (see `read-url.sh`).

## 5. WeChat / Feishu / X

### WeChat `mp.weixin.qq.com`

- **Best effort**: `read-url.sh` tries **`r.jina.ai` then `defuddle.md`**; ignores generic paywall rules on in-body «login» ads (`weixin_nonempty_ok` in script).
- **Not guaranteed**: anti-bot, login-only, deleted articles.
- **Fallback**: Firecrawl MCP, browser copy, export PDF → `pdf-extract.sh` or paste to agent.

### Feishu / Lark `feishu.cn`, `larksuite.com`

- **With app creds**: set **`FEISHU_APP_ID`**, **`FEISHU_APP_SECRET`**, scopes **`docx:document:readonly`**, **`wiki:wiki:readonly`** → `read-url.sh` prefers **`scripts/fetch_feishu.py`** (docx or wiki→docx).
- **Deps**: `pip install requests` (`scripts/requirements-read.txt`).
- **Not supported**: legacy **`/docs/`** links (convert to **docx** in Feishu first).
- **Without creds**: same as generic web (often login-walled); configure API or MCP/export.

```bash
export FEISHU_APP_ID=cli_xxx
export FEISHU_APP_SECRET=xxx
bash "${READ_ROOT}/scripts/read-url.sh" "https://xxx.feishu.cn/docx/AbCdEfGh"
# or:
python3 "${READ_ROOT}/scripts/fetch_feishu.py" "https://..."
```

### X / Twitter

| Scenario | Script | Note |
|----------|--------|------|
| X / Twitter | `read-url.sh` | Often fails or empty shell; do not invent body text |

## 6. Agent side: MCP first

If **Firecrawl** or another fetch MCP works better (heavy JS, anti-bot), **use MCP first**; scripts are fallback and reproducible pipeline.

## 7. Output wrapper (convention)

Agent adds header around stdout (script does not require it):

```text
Title:  (from first line or <title>)
Author: (if known)
Source: web | github | pdf
URL:    original

Content
{read-url.sh or MCP output}
```

## 8. Troubleshooting

| Symptom | Fix |
|---------|-----|
| defuddle / jina empty | try curl; then MCP; report failure honestly |
| GitHub private 404 | `gh auth login` and retry |
| PDF missing deps | install poppler or `pip install -r scripts/requirements-read.txt` |
| URL contains `&` | **quote the URL** in shell |

## 9. Self-contained

This directory and `read/scripts/` are maintained in this repo only; not tied to other host skill paths.
