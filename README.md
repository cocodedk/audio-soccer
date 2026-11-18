# Audio Soccer

Audio Soccer is a football game you play by ear, for blind and low-vision players. In three levels
you find the ball, score a goal and beat the keeper.

It is an Android app that speaks for itself, in English or Persian. A free trial of the same three
levels plays in a web browser on a computer.

## Website

- [Audio Soccer app page, in English](https://soccer.cocode.dk/app/)
- [Audio Soccer app page, in Persian (فارسی)](https://soccer.cocode.dk/fa/app/)
- [Browser trial, played with a keyboard](https://soccer.cocode.dk/)

## Get the Android app

The app is not published yet. The first release is being prepared, and so is its listing on
F-Droid, an app store for free and open-source Android apps. When they are ready, the
[Audio Soccer app page](https://soccer.cocode.dk/app/) will link to both.

The app needs Android 7.0 or newer.

## Play the browser trial

The [browser trial](https://soccer.cocode.dk/) needs a computer with a keyboard; it does not work on
a phone. Speakers work, and headphones are optional.

1. Open the trial.
2. Under **Language**, choose English or فارسی (Persian). Everything the trial shows and says
   follows it. If your browser has no Persian voice, the trial switches to your screen reader by
   itself and says so.
3. Under **Sound**, choose **Speakers** (the default) or **Headphones**. With headphones, a
   **Headphone check** button appears: it plays knocks on your left while a voice says "Left",
   then the same on your right.
4. Press **Learn the sounds** to hear each sound named.
5. Under **Who reads the game to you**, choose **Built-in voice** or **My screen reader speaks**.
   You can also set the **Speech speed**.
6. Press **Level 1: Find the ball**. Focus moves into the game, and the clock starts when you
   first move.
7. After a level, press **Copy results** to copy a report of your rounds.

The keys during play:

- Arrow keys: run. Up is toward the goal.
- Space: kick straight up the pitch. Hold Left or Right while you kick to angle it 15 degrees to
  that side.
- W: hear where you are.
- G: give up the round.
- H: hear the keys again.
- `[` and `]`, or `-` and `+`: slower or faster speech.
- Escape: pause.

If the arrow keys read the page instead of moving you, your screen reader needs to pass the keys to
the game. The trial page gives two tips: in NVDA or JAWS, press NVDA+Space or turn on forms mode; in
VoiceOver, turn Quick Nav off by pressing Left and Right together.

**Clear results** deletes the results stored on your computer. Press it once, then again within 15
seconds to confirm. The level in progress is kept.

### Phone controls test

The [phone controls test](https://soccer.cocode.dk/phone/) is an earlier test of touch controls,
in an Android phone's browser. You play on the phone's speaker with the screen reader off.

1. With your screen reader on, choose English or فارسی and press **Start**. The game checks that
   its own voice can speak. If no voice speaks, the page says so and you keep the screen reader on.
2. When the game asks, turn your screen reader off, then swipe up in the middle of the screen.
3. In the spoken menu, swipe right or left to move between **Drag to steer**, **Tilt to steer**
   and **Finish**. Double-tap to choose.
4. Each way of steering plays the trial's level 2 for three rounds. Tap to kick, tap with two
   fingers to hear where you are, and tap with three to go back to the menu. The phone buzzes once
   when the ball comes into reach and twice when you line up with the goal.
5. Choose **Finish**, then turn your screen reader back on. Your results are on the page, with a
   **Copy results** button.

## How the app is played

You play on the phone's speaker with the screen reader turned off, and the game speaks for itself.
It speaks through the speech engines installed on your phone, for example eSpeak NG, and you choose
one in Settings. If your phone has no speech engine, install one first.

While the screen reader is on, the menus are ordinary buttons it reads, so you can set things up
first. If the screen reader is still on when a level is about to start, the game asks you to turn it
off and waits. When you finish, you turn it back on yourself.

With the screen reader off, the game speaks its own menus:

- Hold a finger on the screen to hear an option.
- Swipe to move between options: quickly with one finger, or at any speed with two.
- Tap once to hear the current option again.
- Double-tap to choose.

During play:

- Drag a finger anywhere on the screen to run. Up the screen is toward the goal.
- Tap to kick.
- Tap with two fingers to hear where you are.
- Tap with three fingers to pause.

The phone vibrates when the ball comes into reach and when you line up with the goal. A fanfare
plays for each round you win. Each level remembers your best time and whether you won all three
rounds: its menu option tells you, and the summary at the end of the level tells you when you set a
new record.

If you can see some of the screen, the menus are white text on black with the option in focus in
yellow, and the pitch is drawn in bright colours on black: you in cyan, the ball in orange, the
keeper in pink and the goal in yellow.

## Privacy

The app has no internet permission, no ads, no tracking and no analytics. It uses two permissions:
one to make the phone vibrate, and one added by an Android library that only apps signed with the
same key can hold.

Your settings and records stay on your phone. They are not backed up, and they are deleted when you
uninstall the app or clear its data. Read the full [privacy policy](https://soccer.cocode.dk/privacy/).

## Build from source

You need JDK 17 and the Android SDK. If your default JDK is newer, point `JAVA_HOME` at a JDK 17.
Put the SDK path in `local.properties` as `sdk.dir=...`; that file is not committed.

```
./gradlew buildSmoke
```

This runs the tests with the 100% line coverage gate and lint, and builds the debug APK at
`app/build/outputs/apk/debug/app-debug.apk`. Install the git hooks with `scripts/install-hooks.sh`.

To play the app on an emulator, run `./gradlew buildSmoke` first, then:

- `scripts/emu.sh` starts a headless emulator, installs the debug APK and opens the app. It looks
  for an AVD named `soccer`. If there is none and no `avdmanager` to make one, name an AVD you
  have: `AVD=<name> scripts/emu.sh`.
- `scripts/e2e.sh` plays the game through adb and prints PASS or FAIL for each check. Every verdict
  comes from the app's own log lines. The first run downloads eSpeak NG from F-Droid, to test a
  second speech engine.
- `scripts/emu.sh stop` stops the emulator.

The scripts only talk to the emulator (port 5584 unless `PORT` says otherwise), so a phone on USB
is left alone.

To run the trial on your own computer, start `python3 -m http.server --directory prototype`, then
open `http://localhost:8000/` for the trial or `http://localhost:8000/phone/` for the phone test.
Run the trial's logic tests with `node --test prototype/test/*.test.mjs`.

## Releases

Nothing is published yet. Each release will be a signed `AudioSoccer.apk` on this repository's
[Releases page](https://github.com/cocodedk/audio-soccer/releases). The app is meant to go to
F-Droid, which builds it from this source and shows the texts in
[`fastlane/metadata/android/`](fastlane/metadata/android/).

1. Once, the owner runs `scripts/setup-signing.sh` to create the signing key and the four
   repository secrets: `KEYSTORE_BASE64` (the keystore, base64-encoded), `KEYSTORE_PASSWORD`,
   `KEY_ALIAS` and `KEY_PASSWORD`. Read the top of the script first: the first signed release fixes
   the key for good, because every update must be signed with it.
2. Raise `VERSION_NAME` and `VERSION_CODE` in `gradle.properties`.
3. Run the **Release APK** workflow on `main` from the Actions tab.

The workflow runs the tests and the coverage gate, builds the signed APK and tags
`v<VERSION_NAME>`. It stops before the tag if a secret is missing, if that version is already
released, or if `VERSION_CODE` is not larger than at the last release.

## Repository map

- [`core/`](core/): the game's logic in pure Kotlin, ported from the trial so the numbers and the
  spoken lines match it.
- [`app/`](app/): the Android app around that logic.
- [`prototype/`](prototype/): the browser trial, plain JavaScript modules with no build step. The
  phone controls test is in [`prototype/phone/`](prototype/phone/).
- [`site/`](site/): the rest of the website: the app pages in English and Persian, the privacy page,
  and the trial's page head, footer, share image and sitemap.
- [`fastlane/metadata/android/`](fastlane/metadata/android/): the store texts F-Droid reads, in
  English and Persian.
- [`docs/DESIGN.md`](docs/DESIGN.md): the game design.
- [`docs/overview.html`](docs/overview.html): an explainer of the planned full game, with a sample
  attack and a sound gallery.
- [`site/privacy/index.html`](site/privacy/index.html): the app's privacy policy, published at
  <https://soccer.cocode.dk/privacy/> (Persian: `site/fa/privacy/`).
- [`docs/persian-to-check.md`](docs/persian-to-check.md): the app's Persian text, waiting for a
  native speaker's check.
- [`scripts/`](scripts/): the file-length check, the git hooks installer and the emulator scripts.

---

Made by [Babak Bandpey](https://linkedin.com/in/babakbandpey) at [Cocode](https://cocode.dk).
Licensed under [Apache-2.0](LICENSE).
