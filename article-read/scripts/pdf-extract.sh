#!/usr/bin/env bash
# Extract plain text from a local PDF path. stdout = text; stderr = errors.
set -euo pipefail
PDF="${1:?usage: pdf-extract.sh /path/to/file.pdf}"
[[ -f "$PDF" ]] || { echo "not a file: $PDF" >&2; exit 1; }

if command -v pdftotext >/dev/null 2>&1; then
  exec pdftotext -layout "$PDF" -
fi

if command -v python3 >/dev/null 2>&1; then
  exec python3 - "$PDF" <<'PY'
import sys
path = sys.argv[1]
try:
    from pypdf import PdfReader
except ImportError:
    try:
        from PyPDF2 import PdfReader
    except ImportError:
        print("install poppler (pdftotext) or: pip install pypdf", file=sys.stderr)
        sys.exit(1)
r = PdfReader(path)
parts = []
for p in r.pages:
    t = p.extract_text()
    if t:
        parts.append(t.strip())
print("\n\n".join(parts))
PY
fi

echo "need pdftotext (poppler) or python3+pypdf" >&2
exit 1
