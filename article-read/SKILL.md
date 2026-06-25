---
name: article-read
description: Fetch URL or PDF as clean Markdown for reading, citation, or archive (读). Modes convert (default), summarize, save, preview. Routes CN platforms, GitHub, PDF, proxy cascade, MCP fallback. Use for 读链接, 抓取正文, 提取文章; not article drafts, topic learning workspace, or codebase audit.
---

# Article Read

**Role: 读** — URL / PDF → Markdown for reading, citation, or archive. Scripts under `scripts/`; policy under `references/`.

## Hard rules

1. **Quote URLs** in shell.
2. **Repo text** (non-PDF) → editor Read tool.
3. **Never invent** body on fetch failure.
4. **Paywall/login wall** → reject; see `references/cascade.md`.
5. **One mode** per run unless user sequences (e.g. convert then summarize).

## Modes

| Mode | User signals | Read |
|------|--------------|------|
| **convert** (default) | 读一下, 抓取, fetch URL | `references/workflow.md` |
| **summarize** | 摘要, 总结 + URL | `references/summarize.md` |
| **save** | 保存, save to file | `references/output.md` § save |
| **preview** | 预览, 太长 | `references/output.md` § length |

## Workflow (convert)

1. **Intake** — `references/workflow.md` (classify input).
2. **Route** — `references/router.md` → domain refs if needed.
3. **Fetch**:

```bash
READ_ROOT="<path-to>/article-read"
bash "${READ_ROOT}/scripts/read.sh" "https://…"
```

4. **Deliver** — wrapper in `references/output.md`.
5. **On failure** — `references/vet.md` + `references/mcp-fallback.md`.

## Script surface

| Script | Purpose |
|--------|---------|
| `scripts/read.sh` | Main fetch |
| `scripts/save.sh` | stdin → `~/Downloads/{slug}.md` |
| `scripts/pdf-extract.sh` | Local PDF text |
| `scripts/feishu-fetch.py` | Feishu Open API (needs creds) |
| `scripts/github-blob-to-raw.py` | GitHub blob → raw URL |
| `scripts/lib/common.sh` | Shared (sourced by read.sh) |

Deps: `scripts/requirements.txt` (`requests` for Feishu).

## Validation

After editing scripts:

```bash
bash -n article-read/scripts/read.sh
bash -n article-read/scripts/save.sh
bash -n article-read/scripts/pdf-extract.sh
bash -n article-read/scripts/lib/common.sh
```

Smoke: `bash article-read/scripts/read.sh "https://example.com"` (expect fetch attempt or documented fallback).

## Boundaries

| Use | Use instead |
|-----|------------|
| Single URL/PDF → MD | Multi-query writing research → `article-write` gather |
| Citation / `topic-learn` `sources/` | Systematic topic mastery workspace → `topic-learn` |
| Optional summarize after fetch | Article drafting → `article-write`; codebase audit → `codebase-audit` |
| Repo text (non-PDF) → editor Read tool | Multi-platform排版导出 |

## References

- `references/workflow.md` — intake, modes, commands
- `references/router.md` — decision tree
- `references/cascade.md` — proxy tiers, paywalls
- `references/domain-cn.md` — 微信、飞书
- `references/domain-stealth.md` — JS / 反爬域名
- `references/domain-github-pdf.md` — GitHub、PDF
- `references/mcp-fallback.md`
- `references/output.md`
- `references/summarize.md`
- `references/vet.md`
- `references/topic-sources.md`
