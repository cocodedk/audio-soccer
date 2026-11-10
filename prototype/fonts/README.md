# Fonts

Self-hosted so the trial page makes no request to a third-party host (a hung font host would stall
the page, and it would see the player's IP). Loaded by the `@font-face` rules at the top of
`../style.css`. Latin subset only; Persian text falls back to the system font.

| File | Family, weight | Source |
| --- | --- | --- |
| `atkinson-hyperlegible-latin-400-normal.woff2` | Atkinson Hyperlegible 400 | `@fontsource/atkinson-hyperlegible@5.3.0` |
| `atkinson-hyperlegible-latin-700-normal.woff2` | Atkinson Hyperlegible 700 | `@fontsource/atkinson-hyperlegible@5.3.0` |
| `big-shoulders-display-latin-800-normal.woff2` | Big Shoulders Display 800 | `@fontsource/big-shoulders-display@5.3.0` |
| `big-shoulders-display-latin-900-normal.woff2` | Big Shoulders Display 900 | `@fontsource/big-shoulders-display@5.3.0` |
| `ibm-plex-mono-latin-500-normal.woff2` | IBM Plex Mono 500 | `@fontsource/ibm-plex-mono@5.3.0` |

Downloaded from `https://cdn.jsdelivr.net/npm/@fontsource/<family>@5.3.0/files/<file>` on 2026-11-09.

All three families are licensed under the SIL Open Font License 1.1
(<https://openfontlicense.org>): Atkinson Hyperlegible by the Braille Institute, Big Shoulders
Display by Patric King and the City of Chicago, IBM Plex Mono by IBM.
