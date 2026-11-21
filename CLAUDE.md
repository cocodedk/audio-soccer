# CLAUDE.md

Project: Audio Soccer, an Android football game for blind and low-vision players, played with the screen reader off:
the game speaks for itself. Kotlin + Jetpack Compose, multi-module (`:core` pure-Kotlin logic +
`:app` Android shell). Built and run entirely from the CLI. The design is in `docs/DESIGN.md`; the
browser prototype in `prototype/` is the source of truth for the simulation, the strings and the
numbers that `:core` ports. Build conventions are those of the sibling app chess-puzzles.

## Binding engineering rules

Non-negotiable. Apply to every change.

### 1. 200-line file cap
No code file may exceed **200 lines** (total, including blanks and comments). Applies to every
`.kt`, `.kts`, script, `.js`, `.mjs`, `.css`, `.html` and `.yml` file (`docs/overview.html` is exempt). When a file approaches the limit, split it by responsibility into
smaller files (extract composables, mappers, helpers, etc.). Enforced by `scripts/check-file-length.sh`
in the pre-push hook and in CI.

### 2. `/simplify` once before every commit
Before each commit, run `/simplify` once over the working changes and apply what it finds. One pass,
not a loop (owner, 2026-11-15): this replaces the chess-puzzles rule that iterates to a fixpoint.

### 3. Every change is reviewed once before it is merged
Review each change once before it is merged: `/code-review` on the branch or pull request, or a
reviewer agent on the task's diff. Fix its findings before the merge.
- Note: `/review` is for GitHub PRs; for a local diff or branch use `/code-review`.

### 4. Pre-push test hook
A git `pre-push` hook MUST run the full test suite (`./gradlew test`) **and** the coverage gate, and
must block the push on any failure. Bypassing it (`git push --no-verify`) is forbidden. Install the
hooks with `scripts/install-hooks.sh`.

### 5. 100% test coverage
Maintain **100% line coverage** across all non-`@Composable` code: `:core`, and `:app` logic (the
platform adapters and the game session). Enforced by the Kover `koverVerify` gate (the build fails
below 100%) wired into `buildSmoke` and the pre-push hook. Every change ships with its tests.
- **Branch** coverage is not gate-enforced: idiomatic Kotlin inline/synthetic constructs emit branch
  stubs unreachable by single-threaded tests.
- **`@Composable`** functions are excluded from the metric (Compose-compiler recomposition branches
  are unreachable by any test); they are still exercised by the Robolectric tests and the emulator.

### 6. Notes stay out of this repo
`memory/` is a symlink into the private `vaults` repo (`~/vaults/audio-soccer/`). Never commit
`memory/` or anything in it here.

## Per-change workflow
code + tests → `/simplify` once → `./gradlew test koverVerify` → commit → review → push (the pre-push
hook re-runs tests + coverage).

## Building
JDK 17 (the host default may be newer): `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew buildSmoke`
runs the coverage gate, lint and the debug APK. The Android SDK path goes in `local.properties`
(not committed). `pre-commit` and `pre-push` source `~/.soccer-env.sh` when it exists, so JAVA_HOME can
live there.
