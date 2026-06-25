# GitHub and PDF

## 1. GitHub

### `raw.githubusercontent.com`

Direct `curl` in `fetch-url.sh`.

### `github.com/.../blob/<ref>/<path>`

1. `url-github-blob-to-raw.py` → raw URL → `curl`
2. If 404 and **`gh`** is authenticated: private raw via  
   `gh api repos/{owner}/{repo}/contents/{path}?ref={ref}` with  
   `Accept: application/vnd.github.raw`

**Private repo 404:** `gh auth login`, retry.

### Gists, releases, non-blob URLs

Use generic chain in [`fetch-chain.md`](fetch-chain.md) or MCP if HTML wrapper only.

## 2. Remote PDF URL

Detected by:

- Path ends with `.pdf` / `.PDF`, or
- `Content-Type` contains `pdf` (HEAD request)

**Order:**

1. `r.jina.ai` on PDF URL
2. Download to temp file → `pdf-extract.sh`

## 3. Local PDF path

Pass path directly to `fetch-url.sh` (must exist on disk):

```bash
bash "${FETCH_ROOT}/scripts/fetch-url.sh" "/path/to/paper.pdf"
```

Or call extractor only:

```bash
bash "${FETCH_ROOT}/scripts/pdf-extract.sh" "/path/to/paper.pdf"
```

**Note:** output is **plain text** (layout preserved with poppler `-layout`), not structured Markdown. Agent may lightly format headings if obvious.

## 4. Dependencies

| Tool | Install |
|------|---------|
| `pdftotext` | `brew install poppler` (macOS) |
| `pypdf` fallback | `pip install -r "${FETCH_ROOT}/scripts/requirements-fetch.txt"` |

Missing both → script exits 1 with install hint on stderr.

## 5. Scanned PDFs

Image-only PDFs return empty or garbage text. Report limitation; suggest OCR or user-provided text — do not hallucinate content.
