# shellcheck shell=bash
# Shared helpers for article-read scripts. Source from read.sh only.

readonly READ_UA="${READ_UA:-Mozilla/5.0 (compatible; zhijunio-article-read/2.0)}"
readonly READ_CURL_TIMEOUT="${READ_CURL_TIMEOUT:-90}"

log_read() {
  local tier="$1" status="$2" method="$3" reason="${4:-}"
  if [[ -n "$reason" ]]; then
    printf '[read] tier=%s status=%s method=%s reason=%s\n' "$tier" "$status" "$method" "$reason" >&2
  else
    printf '[read] tier=%s status=%s method=%s\n' "$tier" "$status" "$method" >&2
  fi
}

paywall_hint() {
  local haystack="$1" l
  l=$(printf '%s' "$haystack" | tr '[:upper:]' '[:lower:]')
  [[ "$l" == *subscribe* || "$l" == *sign\ in* || "$l" == *登录* || "$l" == *继续阅读* \
    || "$l" == *付费内容* || "$l" == *此内容已被发布者删除* ]]
}

nonempty_ok() {
  local body="$1" head
  [[ ${#body} -ge 60 ]] || return 1
  head=$(printf '%s' "$body" | head -n 30)
  ! paywall_hint "$head"
}

weixin_nonempty_ok() {
  local body="$1"
  [[ ${#body} -ge 120 ]]
}

try_curl() {
  curl -sL --max-time "$READ_CURL_TIMEOUT" -A "$READ_UA" "$1" || true
}

try_defuddle() {
  try_curl "https://defuddle.md/${1}"
}

try_jina() {
  try_curl "https://r.jina.ai/${1}"
}

emit_body() {
  printf '%s' "$1"
}
