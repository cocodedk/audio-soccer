# Audio Soccer Android App (v0.1.0) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the browser trial's three levels (find the ball, score, beat the keeper) as a real Android app that a blind player runs with the screen reader off, on the phone's speaker, in English or Persian, fully offline.

**Architecture:** A pure-Kotlin `:core` holds everything that can be tested without a phone: the simulation, the spoken lines, the speech queue, a software sound synthesizer and the gesture recognizers. `:app` is a thin shell: it streams the synthesizer to an `AudioTrack`, speaks through Android `TextToSpeech`, reads touches, the screen-reader state, and draws Compose screens. Every spoken line and game event is logged, so the game can be tested on an emulator without hearing it.

**Tech Stack:** Kotlin, Jetpack Compose, Gradle version catalog, Kover, JUnit Jupiter (`:core`), JUnit4 + Robolectric (`:app`), adb + emulator for the end-to-end script. Build conventions are copied from `/home/cocodedk/0-projects/chess-puzzles`.

**Spec:** `docs/DESIGN.md`, plus the tester's answers of 2026-11-15 (below). The code to port is `prototype/js/` and `prototype/phone/js/`; its tests are `prototype/test/`.

**What the tester asked for (2026-11-15), which this plan adds to the spec:**
- With the screen reader off, a finger on an option speaks it and a double-tap chooses it (touch-to-hear menus).
- The game lists every text-to-speech engine installed on the phone (theirs is eSpeak NG) and lets the player pick one.
- Sounds are built in; no internet is needed to play.

## Global Constraints

- **Port rule:** a Kotlin file named after a JS file ports it function for function (same names, same numbers), and each named JS test file becomes a Kotlin test class with the same cases. The JS is the source of truth for bodies and values; this plan only fixes Kotlin names, types and what is new.
- `:core` uses the Kotlin standard library only (`kotlin.math`, `kotlin.random.Random`): no `java.*`, no Android.
- No `.kt`, `.kts` or `.sh` file over 200 lines. 100% line coverage on all non-`@Composable` code (Kover). Never `--no-verify`.
- Before every commit, run `/simplify` once on the uncommitted changes (owner, 2026-11-15): one pass, not a loop. `CLAUDE.md` rule 2 says this in place of the chess-puzzles fixpoint rule.
- compileSdk 36, minSdk 24, targetSdk 36, JVM 17. Run Gradle with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (the host default is JDK 25).
- applicationId `dk.cocode.soccer` (debug suffix `.debug`), `:core` package `dk.cocode.soccer.core`. `VERSION_NAME=0.1.0`, `VERSION_CODE=1000`, set only in `gradle.properties`.
- Manifest permissions: `VIBRATE` only. No `INTERNET`.
- No audio or voice files in the APK: every sound is synthesized in `:core`, every word is spoken by the phone's `TextToSpeech`. The trial's recorded side-call clips are not carried over.
- Speakers only: mono output, no panning. Headphone panning is out of scope.
- Languages: English and Persian, chosen in the app; first run takes Persian if the system language is Persian, else English. All strings live in `:core`, ported verbatim from `prototype/js/i18n/`. Persian text is never spoken by a non-Persian voice.
- Logging: every spoken line goes to logcat tag `SoccerSpeech`, every game event to `SoccerEvent` (`Log.d`).
- Licence: Apache-2.0, as chess-puzzles (the owner confirms before release).
- One steering scheme: drag. Speech speed is whatever the player set for the engine in Android's own settings; the app has no speed setting.
- Out of scope for 0.1.0: the penalty shootout, the full match, a commentator, tilt steering, sharing or storing results, keyboard and gamepad, headphone mode.

## Review Focus

1. **No speech engine, or the engine fails to start:** the app shows a plain message a screen reader can read and does not ask for the screen reader to be turned off. (Task 8, `TtsVoiceTest.noEngineReportsNoEngine`; Task 9, `SessionTest.noEngineBlocksHandOver`.)
2. **The chosen engine has no Persian voice:** the game speaks English text with an English voice and says so once. (Task 8, `TtsVoiceTest.missingLanguageFallsBackToEnglish`; Task 9, `SessionTest.persianWithoutVoiceSpeaksEnglish`.)
3. **The screen reader comes back on during a round:** the round pauses, the game stops speaking menus, and the pause menu is an ordinary screen. (Task 9, `SessionTest.screenReaderOnPausesRound`.)
4. **The app goes to the background mid-round (call, home button):** the round pauses, sound and speech stop, and coming back lands on the pause menu. (Task 9, `SessionTest.backgroundPausesAndSilences`.)
5. **A finger is still on the screen when a round ends or a menu opens:** that stroke is dropped, nothing is chosen by it, and the next touch works normally. (Task 7, `MenuGesturesTest.cancelDropsTheStroke`; Task 9, `SessionTest.touchHeldAcrossScreensIsIgnored`.)

---

## File Structure

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew*
CLAUDE.md, LICENSE, .gitattributes, .githooks/{pre-commit,commit-msg,pre-push}
scripts/{check-file-length.sh,install-hooks.sh,emu.sh,e2e.sh}
.github/workflows/{ci.yml,release-apk.yml}
core/src/main/kotlin/dk/cocode/soccer/core/
  sim/     Constants.kt Vec.kt World.kt Ball.kt Kicks.kt Levels.kt Game.kt
  text/    Lang.kt GameStrings.kt UiStrings.kt EnGame.kt EnUi.kt FaGame.kt FaUi.kt Describe.kt
  cues/    Cues.kt Speakers.kt LineUp.kt
  speech/  Line.kt SpeechQueue.kt
  audio/   Biquad.kt Voices.kt Limiter.kt Mixer.kt Sfx.kt Soundscape.kt
  input/   Pointers.kt PlayGestures.kt MenuGestures.kt Menu.kt
app/src/main/java/dk/cocode/soccer/
  platform/ AudioOut.kt TtsVoice.kt ScreenReader.kt Buzzer.kt Settings.kt
  game/     Session.kt Screens.kt MenuItems.kt
  ui/       MainActivity.kt MenuScreen.kt PlayScreen.kt PitchView.kt Theme.kt
fastlane/metadata/android/{en-US,fa}/...
```

---

### Task 1: Project scaffold and gates

**Files:** everything in the first three lines of File Structure, `core/build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `ui/MainActivity.kt` (an empty Compose activity), `.github/workflows/ci.yml`. Modify `.gitignore` (add `build/`, `.gradle/`, `local.properties`, `*.jks`).

**Interfaces:** Produces the Gradle task `buildSmoke` (= `koverVerify` + `:app:lintDebug` + `:app:assembleDebug`) and `scripts/check-file-length.sh`.

- [ ] **Step 1:** Load the `android-setup` skill and copy the build files, hooks, scripts and `ci.yml` from chess-puzzles, renaming `chess` to `soccer`. Keep its F-Droid settings: `dependenciesInfo { includeInApk = false; includeInBundle = false }`, R8 and resource shrinking on, `keepDebugSymbols`, signing only when the four `KEYSTORE_*`/`KEY_*` env vars exist. Drop its chess-only dependencies (chesslib, JitPack, splashscreen).
- [ ] **Step 2:** Write `CLAUDE.md` with the six binding rules from chess-puzzles' `CLAUDE.md`, and `LICENSE` (Apache-2.0).
- [ ] **Step 3:** Run `scripts/check-file-length.sh && ./gradlew buildSmoke`. Expected: `BUILD SUCCESSFUL`, and `app/build/outputs/apk/debug/app-debug.apk` exists.
- [ ] **Step 4:** Check the existing Pages deploy still only publishes `prototype/` and `site/`: `cd prototype && npm test` passes and `.github/workflows/deploy-pages.yml` is unchanged.
- [ ] **Step 5:** Commit: `chore: scaffold the Android project`.

### Task 2: The simulation

**Files:** `sim/Constants.kt` (constants.js), `sim/Vec.kt`, `sim/World.kt` (world.js), `sim/Ball.kt` (ball.js), `sim/Kicks.kt` (kicks.js). Tests: `WorldTest`, `KickTest`, `KeeperTest`, `DribbleTest`, `KicksTest` from the JS files of the same names.

**Interfaces (produces):**
```kotlin
data class Vec(val x: Double, val y: Double)          // x toward the goal, y from the player's left to right
fun dist(a: Vec, b: Vec): Double
fun snap8(forward: Double, right: Double): Vec        // one of 8 unit directions, or Vec(0.0, 0.0)
class World(var player: Vec, var ball: Vec, var ballVel: Vec, var hasBall: Boolean, var keeper: Vec?)
sealed interface Event                                 // one subtype per event name world.js and ball.js emit, same fields
fun step(w: World, dt: Double, dir: Vec, kick: Boolean): List<Event>
fun aimedIn(y: Double): Boolean
```

- [ ] **Step 1:** Write the five test classes from the JS tests. Add `VecTest.snap8`: `snap8(1.0, 0.0) == Vec(1.0, 0.0)`, `snap8(1.0, 0.9)` is the 45° unit vector, `snap8(0.0, 0.0) == Vec(0.0, 0.0)`.
- [ ] **Step 2:** Run `./gradlew :core:test`. Expected: compile failure, the types do not exist.
- [ ] **Step 3:** Port the four JS files.
- [ ] **Step 4:** Run `./gradlew :core:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(core): port the pitch simulation`.

### Task 3: Strings and descriptions

**Files:** `text/Lang.kt`, `text/GameStrings.kt` + `EnGame.kt` + `FaGame.kt` (i18n/en.js, fa.js), `text/UiStrings.kt` + `EnUi.kt` + `FaUi.kt`, `text/Describe.kt` (describe.js). Tests: `StringsTest` (strings.test.mjs, i18n.test.mjs), `DescribeTest`, `DescribeFaTest`.

**Interfaces (produces):**
```kotlin
enum class Lang { EN, FA }
interface GameStrings   // one member per leaf of en.js; a leaf that is a JS function becomes a Kotlin function
interface UiStrings     // the menu, settings, hand-over and scheme lines (below)
fun gameStrings(lang: Lang): GameStrings
fun uiStrings(lang: Lang): UiStrings
fun whereText(w: World, s: GameStrings): String
```

`UiStrings` takes its hand-over, scheme and menu lines from `en-phone.js` / `fa-phone.js` with the trim the owner approved on 2026-11-13: the drag intro is two short lines (run, stop, kick); the two- and three-finger gestures are explained only in "How to play"; round 1 has no kicking tip. New members, English text fixed here: `greeting` "Audio Soccer. Touch the screen to hear the options. Double-tap to choose.", `levelNames` "Find the ball" / "Score a goal" / "Beat the keeper", `learnSounds` "Learn the sounds", `howToPlay` "How to play", `settings` "Settings", `language`, `voiceEngine(name)` "Voice: {name}", `resume`, `giveUp`, `backToMenu`, `hearAgain`, `noEngine` "This phone has no speech engine, so the game cannot speak. Install one, for example eSpeak, and open the game again.", `englishOnly` "This voice cannot speak Persian, so the game speaks English."

- [ ] **Step 1:** Write the tests. `StringsTest.everyStringSetInBothLanguages` walks every member of both interfaces for EN and FA and asserts non-blank; `StringsTest.persianNumbersUsePersianDigits` asserts `metresText(12, fa)` contains no ASCII digit.
- [ ] **Step 2:** Run `./gradlew :core:test --tests '*Strings*' --tests '*Describe*'`. Expected: FAIL.
- [ ] **Step 3:** Port. Write the new Persian strings and list them in `docs/persian-to-check.md` for the owner to read before release.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(core): English and Persian strings`.

### Task 4: Speech queue, levels and the game

**Files:** `speech/Line.kt`, `speech/SpeechQueue.kt` (speech.js rules, without its browser workarounds), `sim/Levels.kt` (levels.js), `sim/Game.kt` (game.js). Tests: `SpeechQueueTest` (speech.test.mjs), `GameTest` (game.test.mjs).

**Interfaces (produces):**
```kotlin
enum class Priority { LOW, NORMAL, HIGH }
data class Line(val text: String, val priority: Priority = Priority.NORMAL, val cut: Boolean = false,
                val key: String? = null, val gapMs: Long = 0, val waitMs: Long = 1500,
                val many: Boolean = false, val pitch: Float = 1f)
interface Voice { fun speak(text: String, pitch: Float, onDone: () -> Unit); fun stop() }
class SpeechQueue(private val voice: Voice, private val now: () -> Long) {
    fun say(line: Line); fun tick(); fun clear()
}
enum class Action { KICK, WHERE, GIVE_UP, HELP }
class Game(rand: Random, strings: GameStrings, say: (Line) -> Unit, onLevelDone: (LevelResult) -> Unit) {
    fun startLevel(level: Int); fun update(dt: Double, dir: Vec): List<Event>; fun action(a: Action)
    fun pause(); fun resume(); val world: World?; val level: Int; val running: Boolean
}
data class LevelResult(val level: Int, val rounds: List<RoundStats>, val summary: String)
```

- [ ] **Step 1:** Write the two test classes. `SpeechQueueTest` must include: a HIGH line cuts a speaking NORMAL line; a same-key line replaces the queued one; a line unsaid after `waitMs` is dropped; `gapMs` rate-limits a key.
- [ ] **Step 2:** Run `./gradlew :core:test`. Expected: FAIL.
- [ ] **Step 3:** Port. Physics sub-step 1/120 s, 3 s between rounds, frame dt capped at 0.05 s, round cap 120 s.
- [ ] **Step 4:** Run `./gradlew :core:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(core): speech queue, levels and game`.

### Task 5: The synthesizer

**Files:** `audio/Biquad.kt`, `audio/Voices.kt`, `audio/Limiter.kt`, `audio/Mixer.kt`, `audio/Sfx.kt` (the recipes in `prototype/js/sfx.js:52-115`). Tests: `MixerTest`, `SfxTest`, `LimiterTest` (limiter.test.mjs).

**Interfaces (produces):**
```kotlin
const val SAMPLE_RATE = 44100
enum class Sound { RATTLE, TICK, TAP, KICK, BOARD, END_WALL, BRUSH, STEP, CHIME, POST, SAVE, GOAL, KEEPER_STEP }
interface SoundSink {
    fun play(sound: Sound, gain: Double = 1.0, behind: Boolean = false, stretch: Double = 1.0)
    fun crowd(level: Double)                       // 0.0 stops the crowd loop
}
class Mixer(seed: Long) : SoundSink {
    fun render(out: ShortArray, frames: Int)       // mono 16-bit; thread-safe against play() and crowd()
    fun silence()
}
```

Two building blocks, as in the trial: a noise burst through a biquad (bandpass, lowpass or highpass, RBJ formulas) with a 2 ms attack and exponential decay, and a tone gliding exponentially from f0 to f1 with a 3 ms attack. `behind = true` runs the sound through a 2000 Hz lowpass. `stretch` scales the chime's length (the `k` in sfx.js). The limiter is the trial's compressor (threshold −7.5 dB, ratio 20, attack 3 ms, release 250 ms) followed by a master gain chosen so the test below holds; do not copy the trial's 0.57.

- [ ] **Step 1:** Write the tests:
```kotlin
@Test fun silentMixerRendersZeros()                 // all samples 0
@Test fun everySoundStartsAndEnds()                 // for each Sound: non-zero within 50 ms, all zeros 4 s later
@Test fun peakStaysUnderTheCap()                    // 20 sounds at once at gain 1.0: max |sample| <= 0.9 * 32767
@Test fun tickIsBrightKickIsLow()                   // Goertzel: TICK has more energy at 4800 Hz than at 200 Hz, KICK the reverse
@Test fun gainScalesLoudness()                      // RMS of play(TICK, 0.5) is 0.45..0.55 of play(TICK, 1.0), limiter idle
@Test fun behindRemovesTreble()                     // TICK behind: energy at 4800 Hz under a quarter of TICK ahead
@Test fun crowdLoopsUntilStopped()                  // crowd(0.045) non-zero after 5 s; crowd(0.0) zeros within 1 s
```
- [ ] **Step 2:** Run `./gradlew :core:test --tests '*Mixer*' --tests '*Sfx*' --tests '*Limiter*'`. Expected: FAIL.
- [ ] **Step 3:** Implement. Noise comes from `Random(seed)`, so renders are repeatable.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(core): software synthesizer for the game sounds`.

### Task 6: Cues and the soundscape

**Files:** `cues/Speakers.kt` (speakers.js), `cues/Cues.kt` (cues.js), `cues/LineUp.kt` (phone/js/buzz.js), `audio/Soundscape.kt` (soundscape.js, speakers path only). Tests: `SpeakersTest`, `SpeakersSimTest`, `CuesSideTest`, `SoundscapeTest`.

**Interfaces (consumes):** `World`, `Event`, `Line`, `SoundSink`, `GameStrings`. **Produces:**
```kotlin
fun distanceGain(d: Double): Double                  // max(0.3, 2 / (2 + 0.3 * (max(d, 2) - 2)))
fun beaconEvery(d: Double): Double                   // clamp(0.5 + (d - 6) * 1.5 / 24, 0.5, 2.0) seconds
class Cues(strings: GameStrings, say: (Line) -> Unit, nowMs: () -> Long) { fun reset(w: World); fun update(w: World) }
class Soundscape(sink: SoundSink) { fun reset(level: Int); fun update(w: World, dt: Double, events: List<Event>) }
enum class Buzz(val pattern: LongArray) { TAKE(longArrayOf(0, 40)), LINED_UP(longArrayOf(0, 40, 80, 40)) }
class LineUp { fun update(w: World, events: List<Event>): Buzz? }
```

The side words ("left", "right", "ahead", "behind") are spoken lines here, not clips: `Line(word, Priority.LOW, cut = true, key = "side", waitMs = 800)`, at least 1500 ms apart, never the same word twice in a row, "ahead" skips the gap.

- [ ] **Step 1:** Write the tests from the JS files. `SoundscapeTest` uses a recording `SoundSink`: a still ball 3 m away ticks every 0.2 s and 20 m away every 0.8 s; the chime plays only while the player has the ball; the crowd level is 0.0 on level 1 and 0.045 on level 2.
- [ ] **Step 2:** Run `./gradlew :core:test`. Expected: FAIL.
- [ ] **Step 3:** Port.
- [ ] **Step 4:** Run `./gradlew :core:test`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(core): cues and soundscape for speakers`.

### Task 7: Touch and menu

**Files:** `input/Pointers.kt`, `input/PlayGestures.kt` (the play rules of phone/js/gestures.js), `input/MenuGestures.kt`, `input/Menu.kt` (phone/js/menu.js). Tests: `PlayGesturesTest` (phone-gestures.test.mjs), `MenuGesturesTest`, `MenuTest`.

**Interfaces (produces):**
```kotlin
enum class Phase { DOWN, MOVE, UP, CANCEL }
data class Touch(val id: Int, val x: Float, val y: Float, val tMs: Long)      // x, y in dp
enum class PlayInput { KICK, WHERE, PAUSE }
class PlayGestures { fun on(phase: Phase, t: Touch): PlayInput?; val dir: Vec }
sealed interface MenuInput { data class Focus(val index: Int) : MenuInput; object Next : MenuInput
                             object Prev : MenuInput; object Activate : MenuInput }
class MenuGestures(private val count: () -> Int, private val heightDp: () -> Float) {
    fun on(phase: Phase, t: Touch): MenuInput?; fun tick(nowMs: Long): MenuInput?
}
class Menu(count: Int) { val index: Int; fun focus(i: Int); fun next(); fun prev() }   // stops at the ends
```

Thresholds, in dp and ms: dead zone 24, tap at most 300, fingers landing within 150 form one tap, swipe at least 60, double-tap window 350. Play: one-finger tap kicks, two fingers ask where, three pause. Menu rules (new):
- The options are equal bands down the screen; band = `floor(y / height * count)`, clamped.
- A finger that is still down after 300 ms explores: `Focus(band)` at once, and again each time it crosses into another band.
- A stroke of at least 60 dp lifted within 300 ms is a swipe: right or down is `Next`, left or up is `Prev`.
- A tap followed by a second tap landing within 350 ms of its lift is `Activate`, wherever it lands.
- A single tap gives `Focus(band)` from `tick` once 350 ms pass with no second tap.

- [ ] **Step 1:** Write the tests, one per rule above, plus: `doubleTapDoesNotMoveFocus` (focus on 3, double-tap on band 0 yields only `Activate`) and `cancelDropsTheStroke`.
- [ ] **Step 2:** Run `./gradlew :core:test --tests '*Gestures*' --tests '*Menu*'`. Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the same command, then `./gradlew :core:koverVerify`. Expected: PASS, 100%.
- [ ] **Step 5:** Commit: `feat(core): touch-to-hear menu and play gestures`.

### Task 8: Phone adapters

**Files:** `platform/AudioOut.kt`, `TtsVoice.kt`, `ScreenReader.kt`, `Buzzer.kt`, `Settings.kt`; one Robolectric test class each.

**Interfaces (consumes):** `Mixer`, `Voice`, `Lang`, `Buzz`. **Produces:**
```kotlin
class AudioOut(private val mixer: Mixer) { fun start(); fun stop() }   // AudioTrack, 44100 Hz mono PCM16, USAGE_GAME, own thread
data class Engine(val pkg: String, val label: String)
enum class TtsState { LOADING, READY, NO_ENGINE }
class TtsVoice(context: Context, enginePkg: String?) : Voice {
    val state: StateFlow<TtsState>
    fun engines(): List<Engine>                          // TextToSpeech.getEngines()
    fun useLanguage(lang: Lang): Lang                    // the language it will really speak: FA only if the engine has it
    fun shutdown()
}
class ScreenReader(context: Context) { val on: StateFlow<Boolean> }     // AccessibilityManager touch exploration
class Buzzer(context: Context) { fun buzz(b: Buzz) }
class Settings(context: Context) {                                      // DataStore
    val lang: Flow<Lang>; val enginePkg: Flow<String?>
    suspend fun setLang(lang: Lang); suspend fun setEngine(pkg: String)
}
```

`TtsVoice.speak` uses `QUEUE_FLUSH`, sets the pitch per line, calls `onDone` from `UtteranceProgressListener` (done and error both), and logs the text to `SoccerSpeech`. It never sets a speech rate, so the engine's own setting applies.

- [ ] **Step 1:** Write the tests with Robolectric shadows (`ShadowTextToSpeech`, `ShadowAudioTrack`, `ShadowVibrator`, `ShadowAccessibilityManager`), including the two Review Focus tests named above and `ScreenReaderTest.followsTouchExplorationChanges`.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest`. Expected: FAIL.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run `./gradlew :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 5:** Commit: `feat(app): sound, speech, sensors and settings adapters`.

### Task 9: The session and the screens

**Files:** `game/Screens.kt`, `game/MenuItems.kt`, `game/Session.kt`, `ui/MenuScreen.kt`, `ui/PlayScreen.kt`, `ui/PitchView.kt`, `ui/Theme.kt`, `ui/MainActivity.kt`. Tests: `SessionTest`, `MenuItemsTest`, `MenuScreenTest` (Robolectric Compose rule).

**Interfaces (consumes):** everything above. **Produces:**
```kotlin
sealed interface Screen {
    object Home : Screen; object Settings : Screen; object Help : Screen; object Learn : Screen
    object NoEngine : Screen
    data class HandOver(val level: Int) : Screen       // waiting for the screen reader to go off
    data class Play(val level: Int) : Screen
    data class Paused(val level: Int) : Screen
    data class Summary(val result: LevelResult) : Screen
}
class Session(/* the adapters as interfaces, a clock */) {
    val screen: StateFlow<Screen>; val items: StateFlow<List<String>>; val focus: StateFlow<Int>
    val lastLine: StateFlow<String>
    fun frame(dtSec: Double); fun menu(input: MenuInput); fun play(input: PlayInput)
    fun steer(dir: Vec); fun back(); fun foreground(on: Boolean)
}
```

Behaviour:
- **Home:** the three levels, "Learn the sounds", "How to play", "Settings". **Settings:** language and voice engine; choosing an item steps to its next value and speaks it (the engine item speaks its line in the new engine).
- **Screen reader on:** each option is an ordinary Compose button (`Role.Button`, the option's text) and the game stays silent. **Screen reader off:** the game speaks `greeting` on Home and then whatever `MenuInput` focuses; `Activate` chooses.
- **Starting a level with the screen reader on** goes to `HandOver`: the hand-over line is shown as a polite live region, and the level starts when the screen reader goes off.
- **Play:** the whole screen is the touch surface. Drag steers. System back or a three-finger tap opens `Paused`: "Resume", "Give up this round", "Back to menu". The phone buzzes per `LineUp`. The screen stays on.
- **Summary:** speaks the level summary; "Hear again", "Back to menu".
- **Learn:** names each sound, then plays it, as `prototype/js/learn.js`; any touch returns to Home.
- `PitchView` draws the pitch, goal, player and ball as high-contrast shapes, turned so up the screen is toward the goal, with `lastLine` as text under it. It takes no touches.
- `Session` logs each `Event` and each screen change to `SoccerEvent`.

- [ ] **Step 1:** Write `SessionTest` with fake adapters: the five Review Focus tests, plus `greetsOnHomeWhenScreenReaderOff`, `silentWhenScreenReaderOn`, `focusSpeaksTheOption`, `activateStartsTheLevel`, `handOverStartsWhenScreenReaderGoesOff`, `threeFingerTapPauses`, `engineItemCyclesInstalledEngines`, `summaryAfterThreeRounds`. `MenuScreenTest`: every option is a node with `Role.Button` and its text.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest`. Expected: FAIL.
- [ ] **Step 3:** Implement. `MainActivity` drives `Session.frame` from `withFrameNanos` and forwards pointer events as `Touch` in dp.
- [ ] **Step 4:** Run `./gradlew buildSmoke`. Expected: `BUILD SUCCESSFUL`, coverage 100%.
- [ ] **Step 5:** Commit: `feat(app): self-spoken menus and the three levels`.

### Task 10: Play it blind on the emulator

**Files:** `scripts/emu.sh` (from chess-puzzles: AVD `soccer`, image `system-images;android-34;google_apis;x86_64`, headless, installs and starts `dk.cocode.soccer.debug`), `scripts/e2e.sh`.

`e2e.sh` drives the app with `adb shell input` and reads `adb logcat -d -s SoccerSpeech SoccerEvent`. Each check prints PASS or FAIL and the script exits non-zero on any FAIL:

1. Launch: the speech log has the greeting.
2. A 600 ms press on the second band (`input swipe x y x y 600`): the log has "Score a goal".
3. Two quick taps: the log has the level 2 round intro.
4. A 3 s drag up the screen (`input swipe` 100 px over 3000 ms): the event log shows the player's x grew by at least 8 m.
5. A tap: the event log has a kick.
6. Back key: the log has "Resume". Choose "Back to menu".
7. Install eSpeak NG from F-Droid (`com.reecedunn.espeak`) on the emulator; the voice-engine option now cycles through two engines, and with Persian chosen the speech log shows Persian lines spoken by eSpeak.
8. `adb logcat -d` has no `FATAL EXCEPTION` for the app and no AudioTrack underrun warnings from it.

- [ ] **Step 1:** Write both scripts.
- [ ] **Step 2:** Run `scripts/emu.sh && scripts/e2e.sh`. Fix what fails in the app, with a unit test for each fix.
- [ ] **Step 3:** Run `scripts/e2e.sh` again. Expected: every line PASS.
- [ ] **Step 4:** Commit: `test: end-to-end script that plays the game through adb`.

### Task 11: Release preparation

**Files:** `.github/workflows/release-apk.yml` (from chess-puzzles, APK named `AudioSoccer.apk`), `fastlane/metadata/android/en-US/` and `fa/` (title, short and full description, `changelogs/1000.txt`), `docs/privacy.md`, `README.md`, `docs/DESIGN.md`.

- [ ] **Step 1:** Add the workflow and the store texts. The descriptions say: for blind and low-vision players, played on the speaker with the screen reader off, English and Persian, no internet, no ads, no tracking.
- [ ] **Step 2:** Update `docs/DESIGN.md`: questions 7 and 8 answered (2026-11-15), "Not confirmed yet" removed, touch-to-hear menus and the engine list added, the slices section says the app's first release is the three trial levels and replaces the phone controls test.
- [ ] **Step 3:** Run `./gradlew buildSmoke` and `scripts/e2e.sh`. Expected: both pass.
- [ ] **Step 4:** Commit: `chore: release workflow and store texts for 0.1.0`.

**Needs the owner, not part of this plan's automation:** reading `docs/persian-to-check.md`; confirming Apache-2.0; creating the signing key and the four repository secrets; running the release workflow; trying the APK on a phone with TalkBack; the F-Droid merge request (`fdroid-release` skill); the email to the tester.
