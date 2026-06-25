#!/usr/bin/env bash
# Structural checklist for a topic-learn workspace (heuristic, not prose QA).
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: topic_check.sh <topic-dir> [slug]

Checks TOPIC.yaml, mastery-map, sources/INDEX, main article, interview,
verification fields, and learning-records. Exit 1 on errors; 0 with warnings only.

Example:
  topic_check.sh ~/vault/topics/http-caching
  topic_check.sh ~/vault/topics/http-caching http-caching
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

topic_dir="${1:-}"
slug_arg="${2:-}"

errors=0
warn=0

fail() {
  printf 'FAIL %s\n' "$1"
  errors=$((errors + 1))
}

warn_msg() {
  printf 'WARN %s\n' "$1"
  warn=$((warn + 1))
}

ok() {
  printf 'OK   %s\n' "$1"
}

if [ -z "$topic_dir" ]; then
  usage >&2
  exit 1
fi

if [ ! -d "$topic_dir" ]; then
  fail "not a directory: $topic_dir"
  echo "---"
  printf 'errors=%s warnings=%s\n' "$errors" "$warn"
  exit 1
fi

topic_dir="$(cd "$topic_dir" && pwd -P)"

if [ ! -f "$topic_dir/TOPIC.yaml" ]; then
  fail "missing TOPIC.yaml"
  echo "---"
  printf 'errors=%s warnings=%s\n' "$errors" "$warn"
  exit 1
fi

yaml_get() {
  local key="$1"
  grep -m1 "^${key}:" "$topic_dir/TOPIC.yaml" 2>/dev/null \
    | sed "s/^${key}: *//;s/\"//g;s/'//g" \
    | tr -d '\r' || true
}

slug="$slug_arg"
if [ -z "$slug" ]; then
  slug="$(yaml_get slug)"
fi

if [ -z "$slug" ]; then
  fail "slug not found (pass as arg or set in TOPIC.yaml)"
fi

phase="$(yaml_get phase)"
mode="$(yaml_get mode)"
depth="$(yaml_get depth)"
verified_on="$(yaml_get verified_on)"
code_repo="$(yaml_get code_repo)"

map_file="$topic_dir/${slug}-mastery-map.md"
main_file="$topic_dir/${slug}.md"
interview_file="$topic_dir/${slug}-interview.md"
index_file="$topic_dir/sources/INDEX.md"

ok "topic-dir=$topic_dir slug=$slug phase=${phase:-?} mode=${mode:-?} depth=${depth:-standard}"

if [ ! -f "$map_file" ]; then
  fail "missing mastery-map: ${slug}-mastery-map.md"
else
  ok "mastery-map present"
  if ! grep -qi 'success looks like\|Success looks like' "$map_file"; then
    warn_msg "mastery-map: Success looks like not found"
  fi
  if ! grep -qi 'out of scope\|Out of scope' "$map_file"; then
    warn_msg "mastery-map: Out of scope not found"
  fi
  if ! grep -q '^## Verification' "$map_file"; then
    warn_msg "mastery-map: ## Verification section missing"
  fi
fi

if [ ! -f "$index_file" ]; then
  warn_msg "missing sources/INDEX.md"
else
  link_count=$(grep -cE 'https?://' "$index_file" 2>/dev/null || true)
  if [ "${link_count:-0}" -eq 0 ]; then
    warn_msg "sources/INDEX.md: no http(s) links — L1/L2 may be empty"
  else
    ok "sources/INDEX.md has $link_count link(s)"
  fi
fi

if [ ! -f "$main_file" ]; then
  warn_msg "missing main article: ${slug}.md"
else
  ok "main article present"
fi

if [ ! -f "$interview_file" ]; then
  warn_msg "missing interview: ${slug}-interview.md"
else
  followups=$(grep -cE '^[0-9]+\.' "$interview_file" 2>/dev/null || true)
  if [ "${followups:-0}" -lt 3 ]; then
    warn_msg "interview: fewer than 3 numbered follow-up questions"
  else
    ok "interview has $followups follow-up line(s)"
  fi
fi

if [ -z "$verified_on" ] || [ "$verified_on" = '""' ]; then
  if [ "$phase" = "shipped" ]; then
    fail "phase=shipped but verified_on is empty"
  else
    warn_msg "verified_on empty (OK before Verify step)"
  fi
else
  ok "verified_on set"
fi

if [ -n "$code_repo" ] && [ "$code_repo" != '""' ]; then
  if find "$topic_dir/examples" -maxdepth 1 -type f ! -name 'README.md' 2>/dev/null | grep -q .; then
    warn_msg "code_repo set but examples/ has non-README files (should be README-only)"
  fi
fi

if [ ! -f "$topic_dir/README.md" ]; then
  warn_msg "missing README.md index"
fi

record_count=$(find "$topic_dir/learning-records" -maxdepth 1 -name '*.md' 2>/dev/null | wc -l | tr -d ' ')
if [ "$phase" = "shipped" ] && [ "${mode:-new}" = "new" ] && [ "${record_count:-0}" -eq 0 ]; then
  warn_msg "phase=shipped mode=new but no learning-records/*.md"
fi

if [ "$depth" = "deep" ]; then
  has_gloss=$([ -f "$topic_dir/${slug}-glossary.md" ] && echo yes || echo no)
  has_ref=$([ -d "$topic_dir/reference" ] && find "$topic_dir/reference" -name '*.md' | grep -q . && echo yes || echo no)
  if [ "$has_gloss" = "no" ] && [ "$has_ref" = "no" ]; then
    warn_msg "depth=deep but no glossary or reference/*.md"
  fi
fi

echo "---"
printf 'errors=%s warnings=%s\n' "$errors" "$warn"
[ "$errors" -eq 0 ]
