# Audio Soccer

**Play the trial:** https://soccer.cocode.dk/

An audio-first 5-a-side football game for blind and visually impaired players. Every pass, tackle
and shot is a sound you can place. The rules borrow from blind football, the Paralympic sport built
around sound.

The real game is an Android app that speaks for itself, so the screen reader can be off during a
match; its first release is in this repository (see [The Android app](#the-android-app)). The browser
trial, also here, answers one question: can a blind player find the ball, take it to the goal and score
using sound alone?

## The Android app

Version 0.1.0 is the trial's three levels as an app: find the ball, score a goal, beat the keeper, three rounds each. A won
round gets a fanfare, and each level's option on the menu says your best time in it. You
play on the phone's speaker with the screen reader turned off. The game speaks its own menus, in English or Persian
(hold a finger on an option to hear it, swipe to move, tap to hear it again, double-tap to choose), through the speech
engines installed on the phone, for example eSpeak.
Settings lets you choose which engine. You steer by dragging a finger anywhere on the screen and tap to kick. The app needs
no internet, has no ads and no tracking, and asks for one permission, vibration. Tilt steering, sharing results and a
speech-speed setting are left out of 0.1.0. See the [privacy policy](docs/privacy.md).

**Build.** You need JDK 17 (the default JDK on your machine may be newer, so point `JAVA_HOME` at a 17) and the Android SDK,
whose path goes in `local.properties` as `sdk.dir=...` (not committed).

```
./gradlew buildSmoke
```

This runs the tests with the 100% line coverage gate and lint, and builds the debug APK at
`app/build/outputs/apk/debug/app-debug.apk`. `:core` is the game's logic in pure Kotlin, ported from the trial so the
numbers and the spoken lines match it, and `:app` is the Android shell around it.

**Play it on an emulator.** Run `./gradlew buildSmoke` first, then:

- `scripts/emu.sh` starts a headless emulator, installs the debug APK and opens the app. It looks for an AVD named
  `soccer`. If there is none and no `avdmanager` to make one, name an AVD you have: `AVD=<name> scripts/emu.sh`.
- `scripts/e2e.sh` plays the game through adb and prints PASS or FAIL for each check. Every verdict comes from the app's own
  log lines. The first run downloads eSpeak NG from F-Droid, to test a second speech engine.
- `scripts/emu.sh stop` stops the emulator.

The scripts only talk to the emulator (port 5584 unless `PORT` says otherwise), so a phone on USB is left alone.

**Releases.** Nothing is published yet. Each release will be a signed `AudioSoccer.apk` on this repository's GitHub Releases
page, and the app is meant to go to F-Droid, which builds from this source; the texts it shows are in
[`fastlane/metadata/android/`](fastlane/metadata/android/). To make a release, raise `VERSION_NAME` and `VERSION_CODE` in
`gradle.properties`, then run the **Release APK** workflow from the Actions tab. It needs four repository secrets,
`KEYSTORE_BASE64` (the signing keystore, base64-encoded), `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. It runs the
tests and the coverage gate, builds the signed APK and tags `v<VERSION_NAME>`. It stops before the tag if a secret is missing
or that version is already released.

## The browser trial

You need a computer with a keyboard and a current browser (Chrome, Edge or Firefox). Speakers work,
and headphones are optional. It works with a screen reader (NVDA, JAWS, VoiceOver) or with the
browser's own voice.

1. Open the trial. Press **Learn the sounds** to hear each sound named.
2. Choose **Sound**: **Speakers** (the default) or **Headphones**. With speakers nothing is panned
   left and right. Distance comes through as volume, the ball's tick and the goal's chime beep faster
   as you close in, and a short recorded word says where your target is, the ball or, once you have
   it, the goal: "ahead", "left", "right" or "behind". The word plays at once, even while your screen
   reader is still talking, so it is never late. With headphones the sounds are placed in 3D (HRTF), and a
   **Headphone check** button appears: you should hear "left" in your left ear, then "right" in your
   right ear. Loud sounds are capped either way. In speakers mode a short line also says "left board"
   or "right board" when you come close to a side board, because one speaker cannot show which side
   the knock is on.
3. Choose **Language**: English or فارسی (Persian). Everything the game shows and says follows it.
   The results you copy and send back stay in English. If your browser has no Persian voice, the
   trial switches to "My screen reader speaks" by itself and says so in Persian, so your own screen
   reader, which has a Persian voice, reads the game. Choosing English again puts your earlier voice
   setting back.
4. Choose whether your screen reader or the browser's voice speaks, and set the speech speed.
5. Start **Level 1: Find the ball**. Focus moves into the game. The clock starts when you first move.
6. Arrow keys run, **Space** kicks, **W** says where you are, **G** gives up the round, **H** repeats
   the keys, **[** and **]** (or **-** and **+**) change the speech speed, **Escape** pauses.
7. After each level, press **Copy results** and send them back. **Clear results** deletes everything
   stored on your computer: press it once, and again within 15 seconds to confirm. The level in
   progress is kept.

If the arrow keys read the page instead of moving the player, switch your screen reader to focus
mode (NVDA: NVDA+Space). With VoiceOver, turn Quick Nav off by pressing Left and Right together.

Three levels, three rounds each: find a still ball, take it to the goal and score, then beat a
keeper. Space never kicks backward: it kicks straight up the pitch, and holding Left or Right angles
it 15° to that side. A shot that is already nearly on target gets a small nudge into the goal, so
lining up with the goal by ear is part of the test.

## Phone controls test

**Try it on an Android phone:** https://soccer.cocode.dk/phone/

A throwaway test, played on the phone's speaker with the screen reader off. It answers which touch
steering works by ear, whether vibration helps, and whether the game's own voice is enough to get
around without the screen reader.

1. With TalkBack on, choose English or فارسی and press **Start**. The game tries up to three voices,
   your language first, until one really starts speaking, and records each try. If no Persian
   voice speaks it speaks English and says so; if none speaks at all the page says so and TalkBack
   stays on.
2. The game asks you to turn TalkBack off and swipe up. TalkBack would keep that swipe for itself, so
   when the page gets it, TalkBack is off and the whole screen becomes one touch surface.
3. A spoken menu: **Drag to steer**, **Tilt to steer**, **Finish**. Swipe right or left to move,
   double-tap to choose.
4. Each scheme plays the trial's level 2, three rounds. Drag from where your finger lands (up the
   screen is toward the goal), or tilt the phone from how you hold it as the round starts. A tap
   kicks, two fingers say where you are, three go back to the menu. The phone buzzes once when the
   ball comes into reach and twice when you are lined up with the goal.
5. **Finish** asks you to turn TalkBack back on and shows the results to copy and send back.

A sighted friend can watch on the screen: it shows where the test is and the gestures that work
now, the menu, the pitch during a round (up the screen is toward the goal), and the last few spoken
lines. It is display only and takes no touches.

To run it locally: `python3 -m http.server --directory prototype`, then open
http://localhost:8000/phone/.

## In this repository

- [`docs/DESIGN.md`](docs/DESIGN.md): the game design.
- [`docs/overview.html`](docs/overview.html): an explainer page with a sample attack and a sound gallery.
- [`prototype/`](prototype/): the browser trial. Plain JavaScript modules, no build step.
  Run the logic tests with `node --test prototype/test/*.test.mjs`.
- [`prototype/phone/`](prototype/phone/): the phone controls test, built on the trial's modules.
- [`core/`](core/) and [`app/`](app/): the Android app, its game logic and its Android shell.
- [`scripts/`](scripts/): the file-length check, the git hooks installer and the emulator scripts.
- [`fastlane/metadata/android/`](fastlane/metadata/android/): the store texts F-Droid reads, in English and Persian.
- [`docs/privacy.md`](docs/privacy.md): the app's privacy policy.
- [`site/`](site/): what GitHub Pages adds around the trial (page head, footer, share image, sitemap).

To run the trial locally: `python3 -m http.server --directory prototype`, then open
http://localhost:8000/.

---

Made by [Babak Bandpey](https://linkedin.com/in/babakbandpey) at [Cocode](https://cocode.dk).
