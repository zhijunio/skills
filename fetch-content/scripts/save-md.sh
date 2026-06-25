#!/usr/bin/env bash
# Save stdin to ~/Downloads/{slug}.md; if exists, use -1, -2, ...
set -euo pipefail
SLUG="${1:?usage: save-md.sh <filename-slug> < content}"
SLUG="${SLUG//[^a-zA-Z0-9._-]/_}"
SLUG="${SLUG#.}"
BASE="${HOME}/Downloads/${SLUG%.md}.md"
out="$BASE"
i=0
while [[ -f "$out" ]]; do
  i=$((i + 1))
  out="${HOME}/Downloads/${SLUG%.md}-$i.md"
done
cat >"$out"
echo "$out"
