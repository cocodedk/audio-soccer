# Audio Soccer: design

Status: second draft, 2026-11-08. Working name, easy to change.

## Why this exists

A blind tester of our chess-puzzles app (an Android TalkBack user) asked for a soccer game. In their
words, visually impaired players "don't have any decent soccer games to play". They want rich audio
effects and a commentator.

Every decision below comes back to one fact: **the player cannot see the screen.** Sound is the
whole game, not a layer on top of a visual one. The tester is the expert on what works for them,
so every slice goes to them early and their feedback outranks this document.

## What success looks like

- A blind player wearing headphones, with TalkBack left on, can install the game, start a match and
  play it to the final whistle without sighted help.
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

The player controls one footballer at a time: whoever on their team is nearest the ball. Control
switches automatically, and the switch is announced with a short sound plus the player's name. The
action set is deliberately tiny:

| Action | With the ball | Without the ball |
|---|---|---|
| Steer | dribble | run toward a spot |
| Kick | never backward. In the attacking third it shoots at the goal, and Left or Right aims inside that post; further out it kicks straight up the pitch, so lining up with the goal stays the player's job. Passing gets its own action, aimed at the teammate who called | — |
| Challenge | — | tackle (moving into the ball carrier also tackles, with an automatic "voy!") |
| Ask | spoken status: score, time, where you are, where the ball is | same |
| Pause | pause menu | same |

Teammates the player isn't controlling run on their own. The difficulty setting changes how good the
opponents are and how fast the game runs. There is also a separate game-speed setting, slow, normal
or fast, so beginners can learn.

## How it sounds

The listener stands on the player's own footballer, always facing the opponent's goal. Headphones
are strongly recommended, and the first launch plays a short left/right check.

- **Left and right**: stereo pan, by where a sound is relative to the player across the pitch.
- **Distance**: volume, and a little reverb for far sounds.
- **Ahead or behind**: stereo can't carry this alone. The game uses proper 3D headphone audio
  (HRTF), which is built into browsers and recent Android. Behind also gets its own cue: sounds
  behind the player are duller. And when the ball passes behind the player, the coach calls
  "behind you!".

**Sound layers, most important first.** When too much is playing, the lower layers duck, never the
upper ones:

1. **Ball**: a continuous rattle while it moves, faster when the ball is faster. Kicks, boards,
   posts and net. Never ducked.
2. **Immediate threats and calls**: "voy!", opponent footsteps close by, teammates calling for the
   ball.
3. **Guide voices**: keeper, coach and goal guide calling short directions ("goal ahead, left!").
   These come from the right place in the stereo field: the goal guide's voice comes from the goal.
4. **Commentator**.
5. **Crowd**. The crowd carries information too: it rises as an attack nears either goal, groans at
   a miss and erupts at a goal.

### Finding your way

A review found five gaps in how the player finds their way by ear. Each one has a fix.

- **A stopped ball is silent.** This is a real problem in blind football. Here a still ball gives a
  soft tick every second or so.
- **The player doesn't know where they are on the pitch.** While the player has the ball, the goal
  they attack sends a short beacon every couple of seconds. Without the ball, the ball is the target
  and the beacon stays quiet. When the player runs close to a side board, it brushes softly on that
  side, and it knocks when they hit it. Crossing into a new third plays a short cue and speaks the
  third's name.
- **Passing needs teammates' positions.** Open teammates call "here!", and the pass aims toward
  whoever called.
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
- Voice: pre-rendered clips from an offline neural text-to-speech engine, stitched together at run
  time (a name clip plus a phrase clip). That means a fixed, made-up roster whose names are all
  rendered in advance. Any engine and voice must have a licence that allows redistribution in an
  open-source app. Human-recorded lines can replace these later.
- **Language: English for v1.** More languages are possible later, but each costs a whole phrase
  library.

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
  pan, volume and filter, and HRTF for 3D placement. The builder chooses between Android's
  built-in audio APIs and Oboe.
- **Fully offline**: no network permission, no ads, no tracking. It ships like our other apps, with a
  GitHub release APK that actually downloads, a privacy policy page and an F-Droid-friendly build.
  Every sound effect must be original, synthesized or CC0.
- **Low vision**: a simple top-down pitch in high contrast, with big dots for players and ball. It is
  optional, and it is never the only place any information appears.

## Building it in slices

Each slice goes to the tester and waits for their feedback before the next one starts.

0a. **Navigation prototype** (built now, in `prototype/`): a browser page for a computer with a
    keyboard and headphones. Three levels: find a still ball, take the ball to the goal and score,
    then beat a keeper. It measures time per round, how often the player asked where they are, and
    how often they hit the boards, so the tester can send results back. It answers whether the
    sounds are enough to play by ear before any Android work.

0b. **Phone controls test** (throwaway): the three input candidates on the tester's phone with
    TalkBack on. Output: the chosen control scheme.

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
4. Do they play with headphones?
5. Which language should the commentator speak?

## Left to the builder

- Survey existing audio sports games (for example on audiogames.net) before slice 1. Note what
  players praise and complain about.
- The exact sound designs, phrase library, AI behaviour, physics tuning and visual style.
- The audio API choice, within the constraints above.
- The detailed rules, within "inspired by blind football".
