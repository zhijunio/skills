# Scripts

| File | CLI |
|------|-----|
| `read.sh` | `read.sh <url-or-local-pdf>` → stdout |
| `save.sh` | `save.sh <slug>` ← stdin → path on stdout |
| `pdf-extract.sh` | `pdf-extract.sh <local.pdf>` |
| `feishu-fetch.py` | needs `FEISHU_APP_ID`, `FEISHU_APP_SECRET` |
| `github-blob-to-raw.py` | `github-blob-to-raw.py <github-blob-url>` |
| `lib/common.sh` | sourced by `read.sh` (not invoked directly) |

Install Python deps: `pip install -r requirements.txt`
