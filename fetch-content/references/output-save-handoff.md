# Output, save, images, tech-mastery handoff

## 1. Agent output wrapper

Scripts print **body only** (Feishu adds YAML front matter). Agent wraps for chat:

```text
Title:  {from <title>, first # heading, or YAML title}
Author: {if known}
Source: web | github | pdf | feishu | wechat | mcp
URL:    {original link}

Content
{body}
```

**Default:** deliver wrapper + content in chat — **no new file** unless user asks save or **tech-mastery** Source needs files.

## 2. Save to disk

```bash
bash "${FETCH_ROOT}/scripts/fetch-url.sh" "https://example.com/page" \
  | bash "${FETCH_ROOT}/scripts/save-md.sh" "example-page"
```

- **`save-md.sh`**: slug alphanumerics `._-` only; writes `~/Downloads/{slug}.md`
- Collision → `-1`, `-2`, …; never overwrite
- Prints **final absolute path** (one line) — capture for topic filing

### tech-mastery Source filing

When **tech-mastery** is active:

1. Fetch each URL via **`fetch-url.sh`** (or MCP — note in plan / source index).
2. Save under `<topic-dir>/sources/` (e.g. `23-work/research/<slug>/sources/`).
3. **Move** downloads into the topic tree to avoid re-fetch.
4. Slug naming: short, source-derived (`jep-403`, `method-api`), stable for citations.

See **`tech-mastery/SKILL.md`**: Source step.

## 3. Content length

| Situation | Handling |
|-----------|----------|
| Too long for one chat message | Truncate with explicit note, or `head -n 200` preview + offer full save |
| User asked save | Full body to file via `save-md.sh` |
| JSON response from tool | Extract body/markdown field first |

## 4. Images

- **Default:** do not bulk-download images.
- User asks for images: extract URLs from Markdown, download to a folder they name, report per-URL success/failure.

## 5. After fetch — what not to do

| User said | Do |
|-----------|-----|
| "Read this link" | Fetch only; no summary unless asked **after** |
| "Summarize this article" (no URL) | Clarify: paste, path, or URL |
| Fetch succeeded | Do not stress "I didn't write a file" when save wasn't requested |

## 6. Handoff to downstream skills

| Next goal | Skill |
|-----------|-------|
| Multi-source **technical** mastery article | **`tech-mastery`** (Source uses fetch-content) |
| De-AI / polish existing draft | **`humanize`** (not fetch-content) |
| Reorder long Markdown | **`rewrite-article`** then **humanize** |

**Chain:** `fetch-content → tech-mastery → rewrite-article? → humanize`

## 7. Troubleshooting

| Symptom | Fix |
|---------|-----|
| defuddle / jina empty | curl hop; then [`mcp-first.md`](mcp-first.md); honest failure |
| GitHub private 404 | `gh auth login`, retry |
| PDF missing deps | poppler or `pip install -r scripts/requirements-fetch.txt` |
| URL contains `&` | Quote URL in shell |
| Feishu login wall | Set `FEISHU_APP_*` or **`lark-doc`** — [`platforms-cn.md`](platforms-cn.md) |
| WeChat deleted / verify | MCP, PDF export, or paste |
