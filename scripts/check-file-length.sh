#!/bin/sh
# Enforce the 200-line file cap (CLAUDE.md rule 1) on code: the extensions in $exts, plus the git hooks.
# Exempt: docs/overview.html (a docs page, not code). Lockfiles and build output are never matched:
# they are .json or gitignored.
# Single source of truth, shared by .githooks/pre-push and CI (.github/workflows/ci.yml).
set -eu
cd "$(git rev-parse --show-toplevel)"

max=200
exts='kt kts sh py js mjs css html yml'
set --
for e in $exts; do set -- "$@" "*.$e"; done
# Untracked files count too, so the check also covers work that is not yet committed.
files=$(git ls-files --cached --others --exclude-standard "$@" .githooks ':(exclude)docs/overview.html')
over=$(printf '%s\n' "$files" | while IFS= read -r f; do
  if [ -n "$f" ] && [ -f "$f" ]; then
    # awk's NR counts a final line even without a trailing newline (wc -l does not).
    n=$(awk 'END { print NR + 0 }' "$f")
    if [ "$n" -gt "$max" ]; then
      printf '  %s (%s lines)\n' "$f" "$n"
    fi
  fi
done)

if [ -n "$over" ]; then
  echo "FAIL: files exceed $max lines (CLAUDE.md rule 1):" >&2
  printf '%s\n' "$over" >&2
  exit 1
fi
echo "file-length: all code files ($exts, git hooks) are within $max lines"
