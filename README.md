# Audio Soccer

**Play the trial:** https://cocodedk.github.io/audio-soccer/

An audio-first 5-a-side football game for blind and visually impaired players. Every pass, tackle
and shot is a sound you can place. The rules borrow from blind football, the Paralympic sport built
around sound.

The real game will be an Android app that works with TalkBack left on. Before that, this repository
holds a browser trial that answers one question: can a blind player find the ball, take it to the
goal and score using sound alone?

## The browser trial

You need a computer with a keyboard, headphones and a current browser (Chrome, Edge or Firefox).
It works with a screen reader (NVDA, JAWS, VoiceOver) or with the browser's own voice.

1. Open the trial. Press **Headphone check**: you should hear "left" in your left ear, then "right"
   in your right ear. Then press **Learn the sounds** to hear each sound named.
2. Choose whether your screen reader or the browser's voice speaks, and set the speech speed.
3. Start **Level 1: Find the ball**. Focus moves into the game. The clock starts when you first move.
4. Arrow keys run, **Space** kicks, **W** says where you are, **G** gives up the round, **H** repeats
   the keys, **[** and **]** (or **-** and **+**) change the speech speed, **Escape** pauses.
5. After each level, press **Copy results** and send them back.

If the arrow keys read the page instead of moving the player, switch your screen reader to focus
mode (NVDA: NVDA+Space). With VoiceOver, turn Quick Nav off by pressing Left and Right together.

Three levels, three rounds each: find a still ball, take it to the goal and score, then beat a
keeper. Space never kicks backward: it kicks straight up the pitch, and holding Left or Right angles
it 15° to that side. A shot that is already nearly on target gets a small nudge into the goal, so
lining up with the goal by ear is part of the test.

## In this repository

- [`docs/DESIGN.md`](docs/DESIGN.md): the game design.
- [`docs/overview.html`](docs/overview.html): an explainer page with a sample attack and a sound gallery.
- [`prototype/`](prototype/): the browser trial. Plain JavaScript modules, no build step.
  Run the logic tests with `node --test prototype/test/*.test.mjs`.
- [`site/`](site/): what GitHub Pages adds around the trial (page head, footer, share image, sitemap).

To run the trial locally: `python3 -m http.server --directory prototype`, then open
http://localhost:8000/.

---

Made by [Babak Bandpey](https://linkedin.com/in/babakbandpey) at [Cocode](https://cocode.dk).
