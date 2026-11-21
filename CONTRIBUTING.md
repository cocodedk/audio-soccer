# Contributing to Audio Soccer

Audio Soccer is a football game for blind and low-vision players, played by ear. Reports from blind and low-vision
players come first. If something in the game is hard to hear, hard to follow or does not respond, please tell me. You do
not need to write code to help.

## Report a problem or an idea

The quickest way is a form on the [new issue page](https://github.com/cocodedk/audio-soccer/issues/new/choose). There are
three:

- **Accessibility problem**: something you could not hear, find or do, with the screen reader on or off.
- **Bug**: the game crashed, froze or did something wrong.
- **Idea**: something you would like the game to do.

Most questions in the forms are optional. A sighted friend may fill in a form for you.

No GitHub account? Email [bb@cocode.dk](mailto:bb@cocode.dk). Say which phone you use, what you were doing and what you
heard.

To report a security problem, do not open an issue. The [security policy](SECURITY.md) says how to report it privately.

## Set up your computer

You need three things:

- **JDK 17.** If your default Java is newer, point `JAVA_HOME` at a JDK 17.
- **The Android SDK.** Put its path in a file named `local.properties` at the top of the project, as
  `sdk.dir=/path/to/Android/Sdk`. Git ignores this file.
- **Node.js**, only to run the browser trial's tests.

## Install the git hooks

The hooks are for people who push straight to cocodedk/audio-soccer. If you work from a fork, skip to
[Working from a fork](#working-from-a-fork).

Run this once after you clone the project:

```
scripts/install-hooks.sh
```

It tells git to use the hooks in `.githooks/` and prints "Hooks installed" when it is done. The hooks catch on your own
computer what CI would reject later:

- **pre-commit** runs Android lint.
- **commit-msg** checks that the first line of each commit message follows
  [Conventional Commits](https://www.conventionalcommits.org/): a type, a colon and a short description, for example
  `feat: a fanfare for every won round`.
- **pre-push** refuses a force-push to `main`. Then it checks that no code file is over 200 lines and runs every test with
  the 100% coverage gate.

The hooks need JDK 17 too. They first read `~/.soccer-env.sh`, if it exists, so you can set `JAVA_HOME` there once, for
example `export JAVA_HOME=/path/to/jdk-17`. If `JAVA_HOME` is still not set, they use
`/usr/lib/jvm/java-17-openjdk-amd64` when that folder exists.

If a hook fails, fix the cause. Do not skip it with `--no-verify`.

These git settings are optional. They keep history tidy:

```
git config pull.rebase true
git config push.autoSetupRemote true
git config core.autocrlf input
```

On Windows, use `core.autocrlf true` instead.

### Working from a fork

The pre-push hook only allows pushes to repositories under github.com/cocodedk, so it refuses a push to your fork. On a
fork, leave the hooks uninstalled and run the same checks yourself before you push:

```
scripts/check-file-length.sh
./gradlew test koverVerify
```

Write commit messages in the Conventional Commits form described above, because CI does not check them. CI runs the
length check and the full build again on your pull request.

## The project rules

Three rules apply to every change. The pre-push hook and CI enforce the first two.

- **No code file over 200 lines.** Every line counts, blank lines and comments too. The rule covers Kotlin, Gradle, shell,
  Python, JavaScript, CSS, HTML and YAML files and the git hooks (`docs/overview.html` is exempt). Split a file by what it does before it reaches the limit.
- **100% line coverage.** Tests run every line of logic: all of `core/` and the logic in `app/`. Functions marked
  `@Composable` are left out of the count, but Robolectric tests and the emulator still run them. Every change brings its
  own tests.
- **One review before merge.** Every pull request gets one review, and what it finds is fixed before the merge.

## Build and test

```
./gradlew buildSmoke
```

This runs every test with the 100% line coverage gate, then lint, and builds the debug app at
`app/build/outputs/apk/debug/app-debug.apk`. CI runs the same command.

While you work, `./gradlew test koverVerify` is faster: tests and coverage only.

If you change `prototype/`, run the browser trial's tests with `node --test prototype/test/*.test.mjs`. The hooks do not
run them, but CI does on every pull request.

## Test by ear, with the screen reader off and on

The game is played with the screen reader off, but a player may open it with the screen reader on. Check both.

### Screen reader off

Turn the screen reader off, then open the game. It speaks its own menus. Hold a finger on the screen to hear an option.
Swipe to move between options: a quick swipe with one finger, or a swipe at any speed with two. Tap once to hear the
current option again, and double-tap to choose it.

In a match, drag a finger anywhere to run; up the screen is toward the goal. Tap to kick, tap with two fingers to hear
where you are, and tap with three fingers to pause. The phone vibrates when the ball comes into reach and when you line up
with the goal.

Everything a change adds must be heard, not only shown on the screen.

### Screen reader on

With the screen reader on, the menus are ordinary buttons that the screen reader reads. Choose a level: the game asks
you to turn the screen reader off and waits until you do. After the level, turn it back on yourself.

### On the emulator

After `./gradlew buildSmoke`, `scripts/emu.sh` starts an emulator, installs the debug app and opens it. It looks for an
emulator named `soccer`; to use another, run `AVD=<name> scripts/emu.sh`. Then `scripts/e2e.sh` plays the game through
adb and prints PASS or FAIL for each check. `scripts/emu.sh stop` stops the emulator. The emulator run never turns a
screen reader on, so check that part on a phone.

### On a phone

Install the debug app with `adb install app/build/outputs/apk/debug/app-debug.apk`. Play on the phone's speaker. In the
pull request, name the phone and its Android version.

## Branches and pull requests

Never commit to `main`. Make a branch, push it and open a pull request against `main`.

Start the branch name with the type of change, the same word your commit messages start with. Follow it with a few
lower-case words joined by hyphens, for example `feat/two-finger-menu` or `fix/learn-first-sound-delay`. The types are
`feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `ci` and `build`.

The pull request form asks what changed, how you tested it, and whether you checked with the screen reader off and on.
If you add or change Persian text, list it in [the Persian check list](docs/persian-to-check.md) next to its English, so
a native reader can check it before a release.
