# Audio Soccer

**Play the trial:** https://soccer.cocode.dk/

An audio-first 5-a-side football game for blind and visually impaired players. Every pass, tackle
and shot is a sound you can place. The rules borrow from blind football, the Paralympic sport built
around sound.

The real game will be an Android app that speaks for itself, so the screen reader can be off during
a match. Before that, this repository holds a browser trial that answers one question: can a blind
player find the ball, take it to the goal and score using sound alone?

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

To run it locally: `python3 -m http.server --directory prototype`, then open
http://localhost:8000/phone/.

## In this repository

- [`docs/DESIGN.md`](docs/DESIGN.md): the game design.
- [`docs/overview.html`](docs/overview.html): an explainer page with a sample attack and a sound gallery.
- [`prototype/`](prototype/): the browser trial. Plain JavaScript modules, no build step.
  Run the logic tests with `node --test prototype/test/*.test.mjs`.
- [`prototype/phone/`](prototype/phone/): the phone controls test, built on the trial's modules.
- [`site/`](site/): what GitHub Pages adds around the trial (page head, footer, share image, sitemap).

To run the trial locally: `python3 -m http.server --directory prototype`, then open
http://localhost:8000/.

---

Made by [Babak Bandpey](https://linkedin.com/in/babakbandpey) at [Cocode](https://cocode.dk).
