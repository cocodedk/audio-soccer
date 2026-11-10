# Side-call clips

The four side calls ("ahead", "left", "right", "behind") in English and Persian, as short recorded
clips. Clips play at once and a newer one cuts the old one off, so a call is never late, even when the
player's screen reader is still reading something. They also need no Persian voice on the player's
computer.

`side-<word>.<lang>.wav`: 22.05 kHz, mono, 16-bit. Silence trimmed, same peak level (-3 dBFS).

| Language | Voice | Licence of the voice's training data | Text rendered |
|---|---|---|---|
| English (`en`) | Piper `en_US-ljspeech-high` | LJ Speech, public domain | Ahead. Left. Right. Behind. |
| Persian (`fa`) | Piper `fa_IR-amir-medium` | Amir dataset (datacula), CC0 | `[[ dʒelˈo ]]`. چپ. راست. `[[ pˈoːʃte sˈar ]]`. |

Two Persian words are rendered from phonemes, because the voice gets them wrong from text. "Ahead":
it reads جِلو as "jelu" and جلو as "jolo", so `[[ dʒelˈo ]]` gives "jelo". "Behind": from پُشتِ سَر its
short "o" sounds like "pushte sar", so `[[ pˈoːʃte sˈar ]]` lengthens it to "poshte sar" (chosen by ear
by the owner). Rendered offline with `piper-tts` on 2026-11-09; human-recorded words can
replace these files at any time under the same names.
