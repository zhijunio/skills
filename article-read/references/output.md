# Output and save

## Chat wrapper

Scripts print **body only** (Feishu may prepend YAML). Agent wraps:

```text
Title:  {first # heading or YAML title}
Source: web | github | pdf | feishu | wechat | mcp
URL:    {original}

Content
{body}
```

Default: wrapper in chat — **no file** unless save mode or topic Source.

## Save

```bash
bash "${READ_ROOT}/scripts/read.sh" "https://example.com/page" \
  | bash "${READ_ROOT}/scripts/save.sh" "example-page"
```

- Writes `~/Downloads/{slug}.md`; collision → `-1`, `-2`, …
- Prints final absolute path (one line)

### Topic sources

When topic research is active:

1. Fetch per URL via `read.sh` (or MCP — note method).
2. Save under `<topic-dir>/sources/`.
3. Stable slugs (`jep-403`, `method-api`).

## Length

| Case | Handling |
|------|----------|
| Too long for chat | Truncate + note, or save full file |
| preview mode | `head -n 200` + offer save |
| JSON from tool | Extract markdown field first |

## Images

Default: no bulk download. User asks → download to named folder, report per URL.

## Troubleshooting

| Symptom | Next |
|---------|------|
| proxy tier all fail | `mcp-fallback.md` |
| GitHub private 404 | `gh auth login` |
| PDF deps missing | poppler or `pip install -r scripts/requirements.txt` |
| URL has `&` | Quote in shell |
| Feishu wall | `FEISHU_APP_*` or lark-doc |
