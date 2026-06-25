# Workflow

## Intake card

```text
input:     URL | local .pdf path
mode:      convert | summarize | save | preview
save_slug: (save mode) filename slug
topic_dir: (optional) for sources/ filing
```

Classify URL host → load **one** domain reference if not generic.

## Convert (default)

1. Run `read.sh` with quoted URL.
2. Wrap stdout per `output.md`.
3. Do not summarize.

```bash
bash "${READ_ROOT}/scripts/read.sh" "https://example.com/post"
```

## Save

```bash
bash "${READ_ROOT}/scripts/read.sh" "https://example.com/post" \
  | bash "${READ_ROOT}/scripts/save.sh" "example-post"
```

Capture printed path for topic filing (`output.md`).

## Preview

After successful fetch: `head -n 200` + note「全文已抓取，需要保存请说 save」.

## Summarize

See `summarize.md` — **fetch first**, then summary; never skip fetch when URL given.

## stderr contract

Scripts emit structured lines:

```text
[read] tier=<github|feishu|wechat|proxy|pdf|local> status=<ok|fail> method=<name> [reason=...]
```

Include tier summary in agent report on failure.

## Environment

| Var | Purpose |
|-----|---------|
| `FEISHU_APP_ID` / `FEISHU_APP_SECRET` | Feishu Open API |
| `READ_UA` | Override User-Agent |
| `READ_CURL_TIMEOUT` | Seconds (default 90) |
