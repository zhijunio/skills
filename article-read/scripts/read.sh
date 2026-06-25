#!/usr/bin/env bash
# read.sh — URL or local PDF → Markdown on stdout. Exit 0 on non-empty body.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"

URL_RAW="${1:?usage: read.sh <url-or-local-pdf-path>}"

# --- local PDF ---
if [[ -f "$URL_RAW" ]]; then
  if [[ "$URL_RAW" == *.pdf ]] || file -b --mime-type "$URL_RAW" 2>/dev/null | grep -qi pdf; then
    log_read local ok pdf-extract
    exec "${SCRIPT_DIR}/pdf-extract.sh" "$URL_RAW"
  fi
  log_read local fail file reason="not-a-pdf"
  echo "read.sh: local file is not a PDF: $URL_RAW" >&2
  exit 1
fi

URL="$URL_RAW"
if [[ "$URL" != http://* && "$URL" != https://* ]]; then
  log_read local fail input reason="not-url-or-pdf"
  echo "read.sh: not a remote URL or existing local .pdf: $URL" >&2
  exit 1
fi

body=""

# --- raw.githubusercontent.com ---
if [[ "$URL" == *"raw.githubusercontent.com"* ]]; then
  body=$(try_curl "$URL")
  if nonempty_ok "$body"; then
    log_read github ok raw
    emit_body "$body"
    exit 0
  fi
  log_read github fail raw reason="empty-or-paywall"
fi

# --- github blob → raw ---
if [[ "$URL" == *"github.com"* && "$URL" == *"/blob/"* ]]; then
  raw=$(python3 "${SCRIPT_DIR}/github-blob-to-raw.py" "$URL" 2>/dev/null || true)
  if [[ -n "${raw:-}" ]]; then
    body=$(try_curl "$raw")
    if nonempty_ok "$body"; then
      log_read github ok blob-to-raw
      emit_body "$body"
      exit 0
    fi
  fi
  if command -v gh >/dev/null 2>&1 && [[ "$URL" =~ github\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.+) ]]; then
    owner="${BASH_REMATCH[1]}"
    repo="${BASH_REMATCH[2]}"
    ref="${BASH_REMATCH[3]}"
    gpath="${BASH_REMATCH[4]%%\?*}"
    gpath="${gpath%%#*}"
    body=$(gh api "repos/${owner}/${repo}/contents/${gpath}?ref=${ref}" -H "Accept: application/vnd.github.raw" 2>/dev/null || true)
    if nonempty_ok "$body"; then
      log_read github ok gh-api
      emit_body "$body"
      exit 0
    fi
  fi
  log_read github fail blob reason="empty-or-paywall"
fi

# --- remote PDF ---
is_pdf_url=false
[[ "$URL" == *.pdf || "$URL" == *.PDF ]] && is_pdf_url=true
if ! $is_pdf_url; then
  ct=$(curl -sIL --max-time 25 "$URL" | tr -d '\r' | awk -F': ' 'tolower($1)=="content-type"{print tolower($2); exit}')
  [[ "$ct" == *pdf* ]] && is_pdf_url=true
fi

if $is_pdf_url; then
  body=$(try_jina "$URL")
  if nonempty_ok "$body"; then
    log_read pdf ok jina
    emit_body "$body"
    exit 0
  fi
  tmp=$(mktemp -t readpdfXXXXXX.pdf)
  trap 'rm -f "$tmp"' EXIT
  if curl -sL --max-time 120 -o "$tmp" "$URL"; then
    log_read pdf ok download-extract
    "${SCRIPT_DIR}/pdf-extract.sh" "$tmp"
    exit 0
  fi
  log_read pdf fail download
fi

# --- Feishu / Lark ---
if [[ "$URL" == *"feishu.cn"* || "$URL" == *"larksuite.com"* ]]; then
  if [[ -n "${FEISHU_APP_ID:-}" && -n "${FEISHU_APP_SECRET:-}" && -f "${SCRIPT_DIR}/feishu-fetch.py" ]]; then
    body=$(FEISHU_APP_ID="$FEISHU_APP_ID" FEISHU_APP_SECRET="$FEISHU_APP_SECRET" \
      python3 "${SCRIPT_DIR}/feishu-fetch.py" "$URL" 2>/dev/null || true)
    if [[ ${#body} -ge 80 ]]; then
      log_read feishu ok open-api
      emit_body "$body"
      exit 0
    fi
    log_read feishu fail open-api reason="short-or-error"
  else
    log_read feishu fail creds reason="missing-FEISHU_APP_ID/SECRET"
    echo "read.sh: Feishu/Lark — set FEISHU_APP_ID and FEISHU_APP_SECRET (see references/domain-cn.md)" >&2
  fi
fi

# --- WeChat: jina → defuddle ---
if [[ "$URL" == *"mp.weixin.qq.com"* ]]; then
  body=$(try_jina "$URL")
  if weixin_nonempty_ok "$body"; then
    log_read wechat ok jina
    emit_body "$body"
    exit 0
  fi
  log_read wechat fail jina reason="short"
  body=$(try_defuddle "$URL")
  if weixin_nonempty_ok "$body"; then
    log_read wechat ok defuddle
    emit_body "$body"
    exit 0
  fi
  log_read wechat fail defuddle reason="short"
fi

# --- generic: defuddle → jina → curl ---
for method in defuddle jina curl; do
  case "$method" in
    defuddle) body=$(try_defuddle "$URL") ;;
    jina) body=$(try_jina "$URL") ;;
    curl) body=$(try_curl "$URL") ;;
  esac
  if nonempty_ok "$body"; then
    log_read proxy ok "$method"
    emit_body "$body"
    exit 0
  fi
  log_read proxy fail "$method" reason="empty-or-paywall"
done

echo "read.sh: all tiers failed for: $URL" >&2
exit 1
