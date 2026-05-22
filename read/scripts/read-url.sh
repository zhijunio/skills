#!/usr/bin/env bash
# Fetch URL or local .pdf → Markdown-ish text on stdout. Exit 0 on non-empty body.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
URL_RAW="${1:?usage: read-url.sh <url-or-path>}"

paywall_hint() {
  local haystack="$1"
  local l
  l=$(printf '%s' "$haystack" | tr '[:upper:]' '[:lower:]')
  [[ "$l" == *subscribe* || "$l" == *sign\ in* || "$l" == *登录* || "$l" == *继续阅读* \
    || "$l" == *付费内容* || "$l" == *此内容已被发布者删除* ]] && return 0
  return 1
}

nonempty_ok() {
  local body="$1"
  [[ ${#body} -ge 60 ]] || return 1
  local head
  head=$(printf '%s' "$body" | head -n 30)
  ! paywall_hint "$head"
}

# WeChat articles: in-body "login" ads are not paywalls; length check only.
weixin_nonempty_ok() {
  local body="$1"
  [[ ${#body} -ge 120 ]] || return 1
  return 0
}

try_curl() {
  curl -sL --max-time 90 -A "Mozilla/5.0 (compatible; zhijunio-read/1.0)" "$1" || true
}

try_defuddle() {
  try_curl "https://defuddle.md/${1}"
}

try_jina() {
  try_curl "https://r.jina.ai/${1}"
}

# --- local file (PDF only) ---
if [[ -f "$URL_RAW" ]]; then
  if [[ "$URL_RAW" == *.pdf ]] || file -b --mime-type "$URL_RAW" 2>/dev/null | grep -qi pdf; then
    exec "$SCRIPT_DIR/pdf-extract.sh" "$URL_RAW"
  fi
  echo "read-url.sh: local file is not a PDF: $URL_RAW" >&2
  exit 1
fi

URL="$URL_RAW"
if [[ "$URL" != http://* ]] && [[ "$URL" != https://* ]]; then
  echo "read-url.sh: not a remote URL or existing local .pdf: $URL" >&2
  exit 1
fi

body=""

# --- raw.githubusercontent.com ---
if [[ "$URL" == *"raw.githubusercontent.com"* ]]; then
  body=$(try_curl "$URL")
  nonempty_ok "$body" && printf '%s' "$body" && exit 0
fi

# --- github blob → raw ---
if [[ "$URL" == *"github.com"* && "$URL" == *"/blob/"* ]]; then
  raw=$(python3 "$SCRIPT_DIR/url-github-blob-to-raw.py" "$URL" || true)
  if [[ -n "${raw:-}" ]]; then
    body=$(try_curl "$raw")
    nonempty_ok "$body" && printf '%s' "$body" && exit 0
  fi
  if command -v gh >/dev/null 2>&1 && [[ "$URL" =~ github\.com/([^/]+)/([^/]+)/blob/([^/]+)/(.+) ]]; then
    owner="${BASH_REMATCH[1]}"
    repo="${BASH_REMATCH[2]}"
    ref="${BASH_REMATCH[3]}"
    gpath="${BASH_REMATCH[4]%%\?*}"
    gpath="${gpath%%#*}"
    body=$(gh api "repos/${owner}/${repo}/contents/${gpath}?ref=${ref}" -H "Accept: application/vnd.github.raw" 2>/dev/null || true)
    nonempty_ok "$body" && printf '%s' "$body" && exit 0
  fi
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
    printf '%s' "$body"
    exit 0
  fi
  tmp=$(mktemp -t readpdfXXXXXX.pdf)
  trap 'rm -f "$tmp"' EXIT
  if curl -sL --max-time 120 -o "$tmp" "$URL"; then
    "$SCRIPT_DIR/pdf-extract.sh" "$tmp"
    exit 0
  fi
fi

# --- Feishu / Lark: Open API when app credentials are set (docx / wiki→docx) ---
if [[ "$URL" == *"feishu.cn"* || "$URL" == *"larksuite.com"* ]]; then
  if [[ -n "${FEISHU_APP_ID:-}" && -n "${FEISHU_APP_SECRET:-}" && -f "$SCRIPT_DIR/fetch_feishu.py" ]]; then
    body=$(FEISHU_APP_ID="$FEISHU_APP_ID" FEISHU_APP_SECRET="$FEISHU_APP_SECRET" python3 "$SCRIPT_DIR/fetch_feishu.py" "$URL" 2>/dev/null || true)
    if [[ ${#body} -ge 80 ]]; then
      printf '%s' "$body"
      exit 0
    fi
  fi
fi

# --- WeChat: jina then defuddle (skip generic paywall on in-article login ads) ---
if [[ "$URL" == *"mp.weixin.qq.com"* ]]; then
  body=$(try_jina "$URL")
  if weixin_nonempty_ok "$body"; then
    printf '%s' "$body"
    exit 0
  fi
  body=$(try_defuddle "$URL")
  if weixin_nonempty_ok "$body"; then
    printf '%s' "$body"
    exit 0
  fi
fi

# --- generic: defuddle → jina → plain curl ---
body=$(try_defuddle "$URL")
if nonempty_ok "$body"; then
  printf '%s' "$body"
  exit 0
fi

body=$(try_jina "$URL")
if nonempty_ok "$body"; then
  printf '%s' "$body"
  exit 0
fi

body=$(try_curl "$URL")
if nonempty_ok "$body"; then
  printf '%s' "$body"
  exit 0
fi

echo "read-url.sh: all methods failed or paywall/empty for: $URL" >&2
echo "Tried: github raw/gh (if blob), defuddle.md, r.jina.ai, direct curl" >&2
exit 1
