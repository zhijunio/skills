#!/usr/bin/env bash
# Validate skill directories: name match, YAML frontmatter, eval JSON, shell syntax.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

errors=0
warn=0

check_skill() {
  local dir="$1"
  local skill_md="$dir/SKILL.md"
  [ -f "$skill_md" ] || return 0

  local name
  name=$(grep -m1 '^name:' "$skill_md" | sed 's/^name: *//;s/"//g' | tr -d '\r')
  if [ "$name" != "$dir" ]; then
    echo "FAIL $dir: name='$name' != directory"
    errors=$((errors + 1))
  fi

  if ! grep -q '^description:' "$skill_md"; then
    echo "FAIL $dir: missing description"
    errors=$((errors + 1))
  fi

  local lines
  lines=$(wc -l < "$skill_md" | tr -d ' ')
  if [ "$lines" -gt 120 ] && [ "$dir" != "skill-creator" ]; then
    echo "WARN $dir: SKILL.md is $lines lines (prefer ≤120 + references/)"
    warn=$((warn + 1))
  fi

  if [ -d "$dir/evals" ]; then
    for f in "$dir"/evals/*.json; do
      [ -f "$f" ] || continue
      python3 -m json.tool "$f" >/dev/null 2>&1 || {
        echo "FAIL $f: invalid JSON"
        errors=$((errors + 1))
      }
    done
  fi

  if [ -d "$dir/scripts" ]; then
    while IFS= read -r -d '' sh; do
      bash -n "$sh" 2>/dev/null || {
        echo "FAIL $sh: bash -n"
        errors=$((errors + 1))
      }
    done < <(find "$dir/scripts" -name '*.sh' -print0 2>/dev/null)
    while IFS= read -r -d '' py; do
      python3 -m py_compile "$py" 2>/dev/null || {
        echo "FAIL $py: py_compile"
        errors=$((errors + 1))
      }
    done < <(find "$dir/scripts" -name '*.py' -print0 2>/dev/null)
  fi

  if [ "$dir" != "skill-creator" ]; then
    local eval_count=0
    if [ -d "$dir/evals" ]; then
      eval_count=$(find "$dir/evals" -name '*.json' 2>/dev/null | wc -l | tr -d ' ')
    fi
    if [ "$eval_count" -eq 0 ]; then
      echo "WARN $dir: no evals/*.json"
      warn=$((warn + 1))
    fi
  fi
}

for d in */; do
  check_skill "${d%/}"
done

echo "---"
echo "errors=$errors warnings=$warn"
[ "$errors" -eq 0 ]
