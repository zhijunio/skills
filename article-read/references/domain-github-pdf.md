# GitHub and PDF

## GitHub

### `raw.githubusercontent.com`

Direct curl in `read.sh` (`tier=github`).

### `github.com/.../blob/<ref>/<path>`

1. `github-blob-to-raw.py` → raw URL → curl
2. `gh api` + `Accept: application/vnd.github.raw` when authenticated

Private 404 → `gh auth login`, retry.

### Gists, releases, non-blob

Generic `cascade.md` or MCP.

## Remote PDF

Detected by `.pdf` suffix or `Content-Type: pdf`.

Order: jina → download → `pdf-extract.sh`.

## Local PDF

```bash
bash "${READ_ROOT}/scripts/read.sh" "/path/to/paper.pdf"
# or
bash "${READ_ROOT}/scripts/pdf-extract.sh" "/path/to/paper.pdf"
```

Output is plain text (poppler `-layout`), not rich MD.

## Dependencies

| Tool | Install |
|------|---------|
| `pdftotext` | `brew install poppler` |
| `pypdf` | `pip install -r "${READ_ROOT}/scripts/requirements.txt"` |

Scanned PDFs → report OCR limit; do not hallucinate text.
