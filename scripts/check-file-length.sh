#!/bin/sh
# Enforce the 200-line file cap (CLAUDE.md rule 1) on .kt/.kts/.sh/.py + git hooks.
# Single source of truth, shared by .githooks/pre-push and CI (.github/workflows/ci.yml).
set -eu
cd "$(git rev-parse --show-toplevel)"

max=200
# Untracked files count too, so the check also covers work that is not yet committed.
files=$(git ls-files --cached --others --exclude-standard '*.kt' '*.kts' '*.sh' '*.py' .githooks)
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
echo "file-length: all tracked .kt/.kts/.sh/.py + git hooks are within $max lines"
