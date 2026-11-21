#!/bin/sh
set -eu
cd "$(git rev-parse --show-toplevel)"
git config core.hooksPath .githooks
echo "Hooks installed — pre-commit (fast lint), commit-msg (Conventional Commits),"
echo "and pre-push (owner-lock + protected-branch guard + full test/coverage gate) are active."
