# Persian to check before release

Every Persian string the Android app has that the browser trial did not already have, next to its English. They are in
`core/src/main/kotlin/dk/cocode/soccer/core/text/FaUi.kt`. Please read them and say what to change.

Written for a screen reader user: formal "شما", the same words as the trial (صفحه‌خوان، شوت بزنید، دروازه‌بان، مکث),
and ZWNJ where Persian needs it. `eSpeak` stays in Latin letters, as a product name.

## New

| Member | English | Persian |
|---|---|---|
| greeting | Audio Soccer. Touch the screen to hear the options. Double-tap to choose. | فوتبال صوتی. برای شنیدن گزینه‌ها صفحه را لمس کنید. برای انتخاب، دو بار ضربه بزنید. |
| howToPlay | How to play | راهنمای بازی |
| howToPlayLines\[4] | Tap with three fingers to pause. | با سه انگشت ضربه بزنید تا بازی مکث کند. |
| settings | Settings | تنظیمات |
| language | Language: English | زبان: فارسی |
| voiceEngine(name) | Voice: {name} | صدا: {name} |
| resume | Resume | ادامه |
| giveUp | Give up this round | رها کردن این دور |
| noEngine | This phone has no speech engine, so the game cannot speak. Install one, for example eSpeak, and open the game again. | این گوشی موتور گفتار ندارد، برای همین بازی نمی‌تواند حرف بزند. یکی نصب کنید، مثلاً eSpeak، و بازی را دوباره باز کنید. |
| englishOnly | This voice cannot speak Persian, so the game speaks English. | این صدا فارسی حرف نمی‌زند، برای همین بازی انگلیسی حرف می‌زند. |

## Reused word for word from the trial

These come from `prototype/js/i18n/fa.js`, `fa-ui.js` and `fa-phone.js`, which the trial already shipped, so only the
place they are used is new: a menu item, the how-to-play list, the hand-over line. A line that is a trimmed or split
version of a trial line (the first sentence of the hand-over, one half of the two-finger and three-finger line) is
still the same words.

| Member | English | Persian |
|---|---|---|
| levelNames\[0] | Find the ball | پیدا کردن توپ |
| levelNames\[1] | Score a goal | گل زدن |
| levelNames\[2] | Beat the keeper | شکست دادن دروازه‌بان |
| learnSounds | Learn the sounds | آشنایی با صداها |
| howToPlayLines\[0] | Put a finger anywhere on the screen and drag it the way you want to run. | انگشتتان را هر جای صفحه بگذارید و به سمتی که می‌خواهید بدوید بکشید. |
| howToPlayLines\[1] | Up the screen is toward the goal. Lift your finger to stop. | بالای صفحه به سمت دروازه است. انگشتتان را بردارید تا بایستید. |
| howToPlayLines\[2] | Tap to kick, with any finger, even while you steer. | برای شوت، با هر انگشتی ضربه بزنید، حتی وقتی دارید هدایت می‌کنید. |
| howToPlayLines\[3] | Tap with two fingers to hear where you are. | با دو انگشت ضربه بزنید تا بشنوید کجا هستید. |
| howToPlayLines\[5] | A kick goes straight up the pitch. Line up with the goal, within about twenty metres, and kick. | شوت مستقیم به سمت جلوی زمین می‌رود. با دروازه هم‌راستا شوید، تا حدود بیست متر، و شوت بزنید. |
| paused | Paused | مکث |
| backToMenu | Back to menu | برگشت به منو |
| hearAgain | Hear again | شنیدن دوباره |
| handOver | Turn your screen reader off now. | حالا صفحه‌خوان را خاموش کنید. |
| dragIntro\[0] | Put a finger anywhere on the screen and drag it the way you want to run. Up the screen is toward the goal. Lift your finger to stop. | انگشتتان را هر جای صفحه بگذارید و به سمتی که می‌خواهید بدوید بکشید. بالای صفحه به سمت دروازه است. انگشتتان را بردارید تا بایستید. |
| dragIntro\[1] | Tap to kick, with any finger, even while you steer. | برای شوت، با هر انگشتی ضربه بزنید، حتی وقتی دارید هدایت می‌کنید. |
