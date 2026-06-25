#!/usr/bin/env bash
# Bootstrap a topic-learn workspace directory from templates.
set -euo pipefail

usage() {
  cat <<'EOF'
Usage: topic_scaffold.sh <topic-dir> <slug> [language]

Creates TOPIC.yaml, mastery-map, interview stub, sources/INDEX.md,
learning-records/, and README.md skeleton.

Example:
  topic_scaffold.sh ~/vault/topics/http-caching http-caching zh
EOF
}

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

topic_dir="${1:-}"
slug="${2:-}"
language="${3:-zh}"

if [ -z "$topic_dir" ] || [ -z "$slug" ]; then
  usage >&2
  exit 1
fi

skill_root="$(cd "$(dirname "$0")/.." && pwd)"
tpl="$skill_root/templates"

mkdir -p "$topic_dir"/{sources,learning-records,examples,reference}

if [ ! -f "$topic_dir/TOPIC.yaml" ]; then
  sed \
    -e "s/topic-slug/$slug/g" \
    -e "s/language: zh/language: $language/" \
    "$tpl/TOPIC.yaml" >"$topic_dir/TOPIC.yaml"
else
  echo "skip: TOPIC.yaml exists"
fi

map_file="$topic_dir/${slug}-mastery-map.md"
if [ ! -f "$map_file" ]; then
  sed "s/{Topic}/$slug/g" "$tpl/mastery-map.md" >"$map_file"
else
  echo "skip: mastery-map exists"
fi

interview_file="$topic_dir/${slug}-interview.md"
if [ ! -f "$interview_file" ]; then
  cp "$tpl/interview-output.md" "$interview_file"
else
  echo "skip: interview file exists"
fi

index_file="$topic_dir/sources/INDEX.md"
if [ ! -f "$index_file" ]; then
  cat >"$index_file" <<EOF
# Sources — $slug

## L1 — normative

## L2 — official / product

## L3 — maintainer

## L4 — second-hand (supporting only)

## Gaps

## Open
EOF
else
  echo "skip: sources/INDEX.md exists"
fi

readme="$topic_dir/README.md"
if [ ! -f "$readme" ]; then
  cat >"$readme" <<EOF
# $slug

Topic-learn workspace. See \`${slug}-mastery-map.md\` for Mission and map.

## Files

| File | Role |
|------|------|
| \`TOPIC.yaml\` | mode, phase, runtime, verified_on, code_repo |
| \`${slug}-mastery-map.md\` | Mission, map, sources pointer, verification plan |
| \`${slug}.md\` | Main article |
| \`${slug}-interview.md\` | Interview layer |
| \`sources/INDEX.md\` | Layered sources (L1–L4) |

## Reading order

- **Recall:** reference/ → glossary (if present)
- **Explain:** main → interview
EOF
else
  echo "skip: README.md exists"
fi

printf 'scaffolded: %s (slug=%s)\n' "$topic_dir" "$slug"
