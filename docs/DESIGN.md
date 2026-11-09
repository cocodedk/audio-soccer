# Audio Soccer: design

Status: third draft, 2026-11-09: speakers first, and Persian next to English. Working name, easy to change.

## Why this exists

A blind tester of our chess-puzzles app (an Android TalkBack user) asked for a soccer game. In their
words, visually impaired players "don't have any decent soccer games to play". They want rich audio
effects and a commentator.

Every decision below comes back to one fact: **the player cannot see the screen.** Sound is the
whole game, not a layer on top of a visual one. The tester is the expert on what works for them,
so every slice goes to them early and their feedback outranks this document.

## What success looks like

- A blind player listening on the phone's own speaker, with TalkBack left on, can install the game,
  start a match and play it to the final whistle without sighted help. No headphones: the tester
  can't wear them, because they hurt.
- During play they can tell by ear where the ball is, who has it, and where the goal is, without
  stopping to ask.
- It feels like a real match: crowd, whistle, the ball on the boards, a commentator who reacts.
- The tester plays a second match because they want to, not because we asked.

## Goals, non-goals, anti-goals

**Goals (v1)**
- Single-player matches against computer opponents, with three difficulty levels.
- Audio that carries all game information: positions, possession, the goal, time and score.
- A live commentator who makes it exciting and fills in what the sound effects can't say.
- A training mode and a sound gallery, so players learn every sound before a real match.

**Non-goals (v1)**
- Online or local multiplayer.
- Real clubs, players, leagues or tournaments (licensing). Teams and names are made up.
- Career or manager mode, transfers, stats screens.
- 11-a-side matches.
- iOS.
- Rich graphics. There is a simple high-contrast picture for low-vision players and sighted
  friends watching, nothing more.

**Anti-goals: things that make it a failure even if it "works"**
- **Any step needs sight**: first launch, permissions, setup, menus, results, anything.
- **The game leaves the player without speech.** It must never ask the player to suspend TalkBack
  and must never mute the device. A blind player with a silent device and no sighted help nearby is
  stuck. We will not create that situation.
- **Needing headphones.** Anything the player must know has to come through a phone or laptop
  speaker.
- **Sound that hurts.** No sudden loud peaks: the goal roar, the whistle and hard kicks are capped
  near the level of the ball, and a limiter guards the whole mix.
- **So much sound that the ball gets lost.** The ball is the one sound that is always clear.
- **A commentator who lags, rambles or talks over the cues** the player needs to act on.
- Needing an internet connection, an account, ads or tracking.
- Controls that fight TalkBack's gestures.

## The game: 5-a-side, borrowed from blind football

The rules are inspired by **blind football**, the real 5-a-side Paralympic sport, which was designed
around sound from the start. In outline (the builder should check the IBSA rules for detail, though
we don't need to follow them exactly):

- Four outfield players and a goalkeeper per side. The pitch is small, about 40 × 20 m, with boards
  along both sidelines, so the ball rarely goes out.
- The ball rattles when it moves.
- A player going for the ball calls out "voy!" ("I'm going"), so others hear the challenge coming.
- Three voices guide each team: the goalkeeper in the defending third, the coach in the middle
  third, and a guide standing behind the opponent's goal in the attacking third.
- Two halves. Match length is a setting: 3, 5 or 10 minutes per half, with 5 as the default.

This is my call, for three reasons. Ten players are far easier to follow by ear than 22. The boards
keep play flowing and give a satisfying bounce sound. The three guiding voices hand us a ready-made,
realistic way to tell the player where things are. It still presents as a full soccer match, with
stadium, crowd, commentator and a league of made-up teams. **The tester should confirm this**,
because they might want the feel of regular 11-a-side.

### What the player controls

The player controls one footballer at a time, to start with whoever on their team is nearest the
ball. Control moves to another player in two cases only: when the ball is dead, or when the player
asks for it. It never moves in the middle of a run, because a sudden jump of the listener is the most
disorienting thing that can happen in an audio game. When control moves, the listener glides to the
new player over about 300 ms. A switch sound plays and the new player's name follows, with the
side call for the ball ("Holm, ball ahead"). With headphones the switch sound also pans from the
old player to the new one. The action set is deliberately tiny, six actions:

| Action | With the ball | Without the ball |
|---|---|---|
| Steer | dribble | run toward a spot. The player keeps control while running there |
| Kick | straight up the pitch, never backward. Left or Right angle it by 15°. A small aim help only nudges a shot that is already nearly on target into the goal, so lining up with the goal stays the player's job | — |
| Pass | to the teammate who last called "here!" | — |
| Challenge | — | tackle when the ball carrier is in reach (moving into the carrier also tackles, with an automatic "voy!"). Otherwise it asks to switch to the teammate nearest the ball |
| Ask | spoken status: score, time, where you are, where the ball is | same |
| Pause | pause menu | same |

Teammates the player isn't controlling run on their own. The difficulty setting changes only how
good the opponents are. Game speed is its own setting, slow, normal or fast, so beginners can learn.

## How it sounds

The listener stands on the player's own footballer, always facing the opponent's goal.

**Speakers are the default.** The tester can't wear headphones, and a phone's two speakers sit a few
centimetres apart, so left and right mostly blur and 3D placement is lost. Nothing the player needs
may depend on which ear a sound reaches. Each piece of information has a cue that survives one
small speaker:

- **Distance**: the sound of the target speeds up as the player closes in, like a parking sensor.
  A still ball ticks faster as you near it. While you have the ball, the goal's beacon speeds up as
  you near the goal. Volume and a little reverb for far sounds add to it.
- **Direction**: spoken. When the target moves to another side, one word says where it is now:
  "ahead", "left", "right" or "behind". The target is the ball, or the goal while you have the
  ball. "Ahead" for the goal means a straight kick would score. The words are short and never
  repeat back to back, and they wait while a more important line is playing.
- **Ahead or behind**: sounds behind the player are duller, which a single speaker carries. When
  the ball passes behind the player, the coach calls "behind you!".
- **Exact position, on demand**: Ask gives clock positions and metres (see Finding your way).
- **Touch, on the phone**: vibration that costs the ears nothing. A short buzz when the ball is in
  reach, a different one when you are lined up with the goal. The phone controls test (0b) checks
  that the buzzes don't get in TalkBack's way.

**Headphones are an optional extra** for players who like them. A sound setting switches on stereo
placement, and true 3D placement (HRTF) where it is available. They never add information the
speaker player lacks. They only make it easier to hear. The browser has 3D placement built in.
On Android it would need a library (see the last section), so it comes after v1, if players ask.

**Volume.** A limiter sits on the whole mix, and the loudest one-shots (goal roar, whistle, net,
post, hard kicks) are capped close to the ball's level. A "quiet mix" setting lowers the crowd and
the commentator further. The game never asks the player to turn the volume up.

**Sound layers, most important first.** When too much is playing, the lower layers duck, never the
upper ones:

1. **Ball**: a continuous rattle while it moves, faster when the ball is faster. Kicks, boards,
   posts and net. Never ducked.
2. **Immediate threats and calls**: "voy!", opponent footsteps close by, teammates calling for the
   ball.
3. **Guide voices**: keeper, coach and goal guide calling short directions ("goal ahead, left!").
   These are pre-rendered clips (see the commentator's voice below). They say the direction in
   words, so they work on a speaker. With headphones, the goal guide's voice also comes from the goal.
4. **Commentator**.
5. **Crowd**. The crowd carries information too: it rises as an attack nears either goal, groans at
   a miss and erupts at a goal.

### Finding your way

A review found five gaps in how the player finds their way by ear. Each one has a fix.

- **A stopped ball is silent.** This is a real problem in blind football. Here a still ball gives a
  soft tick every second or so.
- **The player doesn't know where they are on the pitch.** While the player has the ball, the goal
  they attack sends a short beacon, every couple of seconds from far away and faster up close.
  Without the ball, the ball is the target and the beacon stays quiet. When the player runs close to
  a side board, it brushes softly, and it knocks when they hit it. On a speaker the brush can't say
  which side, so the side call says it ("left board"). Crossing into a new third plays a short cue
  and speaks the third's name.
- **Passing needs teammates' positions.** Open teammates call "here!", and the pass goes to
  whoever called last.
- **Distance is ambiguous.** A quiet sound can mean far away or just soft. The player's own
  footsteps give a sense of speed, and the goal guide speaks distances.
- **Ask needs answers the player can use at once.** It answers in clock positions, the convention
  blind people already use. 12 o'clock is toward the goal you attack. For example: "Ball at
  2 o'clock, 3 metres. Goal at 1 o'clock, 22 metres."

**Avoiding too much sound.** These cues add up, so the game keeps the mix thin. Only players within
about 10 metres are audible, plus anyone calling. The game starts slow. The layer rules above decide
what gets quieter.

### The commentator

The commentator is part of the interface, not decoration. They say what the effects can't: who has
the ball, fouls, the score, the time, and the drama.

- Short lines during play ("Holm, down the left…"). Longer colour commentary only when play stops.
- A priority queue: goal > shot > foul > pass. A line that is out of date by the time it would play
  is dropped, never played late.
- Ducks under layers 1–3 and never covers them.
- Verbosity setting: full, key moments only, or off.
- Voice: every in-match voice is a pre-rendered clip from an offline neural text-to-speech engine:
  the commentator, keeper, coach, goal guide, "voy!" and "here!". Clips sound the same on every
  phone, need no voice installed, and can be mixed and ducked like any other sound. Live
  text-to-speech can't be mixed that way, and it depends on which voices the phone has. The
  commentator's lines are stitched together at run time (a name clip plus a phrase clip). That
  means a fixed, made-up roster whose names are all rendered in advance. Any engine and voice must
  have a licence that allows redistribution in an open-source app. Human-recorded lines can replace
  these later.
- **Languages: English and Persian (فارسی) for v1**, chosen by the owner on 2026-11-09. The tester
  is Iranian and lives in Iran. Online voices from Microsoft or Google may be blocked there by
  sanctions or filtering, so the voices must ship inside the app and work offline. Each
  language is its own phrase library and its own set of clips, so the clip pipeline renders every
  line twice from one table. For Persian the candidates are Piper's offline `fa_IR` voices. Each
  voice's licence must be checked before use. If none allows redistribution, the owner can record
  the Persian lines. Persian sentences stitch cleanly ("توپ، ساعت ۲، ۳ متر"), so the name and
  number clips work the same way as in English. Menus and settings are ordinary Android strings, so
  TalkBack reads them in Persian with the player's own voice.

## Menus, TalkBack and the match screen

- Menus, settings, results and the sound gallery are ordinary Android screens, fully usable with
  TalkBack, built the same way as in the chess app.
- The match screen voices itself: all match audio comes from the game, and TalkBack stays on. The
  screen must not make TalkBack chatter during play, so it should have one full-screen element with
  a short label, and no live announcements.
- Pause is always one action away, and the pause menu is an ordinary TalkBack screen.

## The hardest unknown: controls with TalkBack on

With TalkBack on, Android turns single-finger touches into exploration and eats swipes. Ordinary
game touch controls won't work, and we've ruled out asking the player to turn TalkBack off. So
**control input must be proven on the tester's own phone before anything else is built.** The only
thing tested earlier is sound navigation: the browser prototype (slice 0a) checks it first, and
controls on the phone come second.

Candidates for the first spike, in my order of preference:

1. **Finger position as a joystick.** A full-screen view reads TalkBack's touch-exploration (hover)
   events. Where the finger rests relative to the screen centre is the steering direction, and a
   double-tap kicks.
2. **Tilt to steer** (accelerometer, unaffected by TalkBack), with double-tap to kick.
3. **A Bluetooth keyboard or gamepad**, which skips the gesture problem entirely. This should be
   supported regardless, as the most reliable option.

The spike also settles where "ask" and "pause" live in the touch scheme. The result picks the
default scheme; the others can stay as options if they work.

## After v1: 3D audio for headphone players

This used to be the second hardest unknown. Since the game is built for speakers, it no longer
blocks anything. Browsers have per-source HRTF built in, which is why the browser trial can place
a sound ahead of or behind a player with headphones. Android has no per-source HRTF for game audio.
Its Spatializer (Android 13 and later) is for multichannel media on supported devices, and
AudioTrack and Oboe don't do per-source HRTF either. If headphone players ask for it after v1,
the candidates are, in my order of preference: **Steam Audio** (has an Android build), **OpenAL
Soft with HRTF** through the NDK, and **Resonance Audio** (archived, a last resort). Until then,
headphones on Android get plain stereo pan.

## How it gets made

My calls:

- **Platform: mobile, decided by the owner on 2026-11-08.** Android, Kotlin and Jetpack Compose, with
  a pure-Kotlin `:core` module and an `:app` module, the same structure and conventions as
  chess-puzzles (200-line files, the test and coverage gates, the review rules in its `CLAUDE.md`).
  A browser version follows only if it comes cheaply. To keep that door open, `:core` uses no
  Android or JVM-only APIs, so Kotlin Multiplatform can compile it to JavaScript later and a browser
  shell only adds sound, keys and drawing around it. Nothing for the browser is set up until it is
  wanted.
- **`:core` is the match**: a deterministic simulation on a fixed tick with a seeded random number
  generator, so a match can be replayed exactly in tests. It emits game events (kick, pass, board
  bounce, tackle, "voy", foul, goal, whistle) plus positions each tick. Commentary selection (event
  → phrase) also lives in `:core`, so it can be tested.
- **`:app` turns state into sound**: an audio engine that places and mixes the layers above, plus
  input and the menus. The engine needs low latency, many simultaneous sounds and per-sound live
  volume and filter, with a limiter on the mix. Android's built-in audio APIs or Oboe (the
  builder's choice) cover that. Stereo pan for headphone players is a small extra on top.
- **Fully offline**: no network permission, no ads, no tracking. It ships like our other apps, with a
  GitHub release APK that actually downloads, a privacy policy page and an F-Droid-friendly build.
  Every sound effect must be original, synthesized or CC0.
- **Low vision**: a simple top-down pitch in high contrast, with big dots for players and ball. It is
  optional, and it is never the only place any information appears.

## Building it in slices

Each slice goes to the tester and waits for their feedback before the next one starts.

0a. **Navigation prototype** (built, in `prototype/`, live at
    https://cocodedk.github.io/audio-soccer/): a browser page for a computer with a keyboard,
    played on speakers by default. A clear "computer only" notice sits at the top. A sound setting
    switches between speakers (no panning, side calls as short recorded clips, beacons that speed
    up as you near them, and a gentle distance fade with a floor, so a far ball stays audible) and
    headphones (3D placement). The side calls are clips, not speech, because a screen reader can't
    drop a line once it is late: a word queued behind a long line came out seconds after the ball
    had moved. Peaks are capped in both. A language setting switches the
    page and the voice between English and Persian. It uses the browser's Persian voice if there is
    one. Otherwise it switches to "My screen reader speaks", so the player's own screen reader
    reads the game in its Persian voice. It has three levels: find a still
    ball, take the ball to the goal and score, then beat a keeper. A "Learn the sounds" step plays
    the main sounds, named by the voice, before the first round. Each round has a give-up key and a
    2-minute cap. The results record start distance, time, path efficiency, how often the player
    asked where they are, board bumps, kicks, wide shots, saves and give-ups, so the tester can send
    them back. The player can set the speech rate and choose the voice. It answers whether the
    sounds are enough to play by ear on speakers before any Android work.

0b. **Phone controls test** (throwaway): the three input candidates on the tester's phone with
    TalkBack on, played on the phone's speaker, plus the two vibration cues. Output: the chosen
    control scheme, and whether the buzzes help.

(The old slice 0c, a 3D audio library test on Android, is dropped: the game no longer depends on 3D
audio. See "After v1".)

1. **Penalty shootout**: five penalties each, taking and saving. As the taker, pick a direction and
   time the shot. As the keeper, listen to the run-up and dive. This proves the audio engine,
   commentator, crowd and controls in a small, fun package, and it ships to the tester fast.
2. **Training pitch**: the player alone with the ball, the goal and the goal guide. Learn to find
   the ball, dribble and shoot. Includes the sound gallery, where every sound can be played with a
   spoken explanation.
3. **Full match**: 5-a-side against computer opponents, with guiding voices, full commentary,
   difficulty and speed settings, and a made-up league of teams to play.

## Questions for the tester

These go in a later email, most important first:

1. Phone or computer? (Already asked. The owner chose mobile; a strong wish for computer would bring the
   browser version forward.)
2. Is blind-football-style 5-a-side OK, or do they want the feel of regular 11-a-side?
3. Have they played audio games they loved or hated, especially sports ones, and why?
4. ~~Do they play with headphones?~~ Answered 2026-11-09: no, headphones hurt their ears. The
   game is built for speakers.
5. ~~Which language should the commentator speak?~~ The owner chose English and Persian on
   2026-11-09. Ask whether the Persian voice sounds right.
6. Are loud sounds a problem on speakers too? Which ones, and is the capped mix comfortable?

## Left to the builder

- Survey existing audio sports games (for example on audiogames.net) before slice 1. Note what
  players praise and complain about.
- The exact sound designs, phrase library, AI behaviour, physics tuning and visual style.
- The audio API choice, within the constraints above.
- The detailed rules, within "inspired by blind football".
