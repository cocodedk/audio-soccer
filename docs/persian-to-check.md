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
| noEngine | This phone has no speech engine, so the game cannot speak. Install one, for example eSpeak. After installing it, open the speech engine's own app once, so it can get its voices ready. Then open the game again. | این گوشی موتور گفتار ندارد، برای همین بازی نمی‌تواند حرف بزند. یکی نصب کنید، مثلاً eSpeak. بعد از نصب، یک بار برنامه خود موتور گفتار را باز کنید تا صدایش آماده شود. بعد بازی را دوباره باز کنید. |
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

## Store texts

The texts F-Droid shows for the app, in `fastlane/metadata/android/fa/`, with the English they come from in
`fastlane/metadata/android/en-US/`. Plain everyday Persian, formal "شما", for people who read it with a screen reader. The
short description may be no longer than 80 characters. `eSpeak` stays in Latin letters. Please read them and say what to change.

| File | English | Persian |
|---|---|---|
| title.txt | Audio Soccer | فوتبال صوتی |
| short_description.txt | Play football by ear, for blind and low-vision players. No internet, no ads. | با گوش فوتبال بازی کنید، مخصوص نابینایان و کم‌بینایان. بدون اینترنت و تبلیغ |
| full_description.txt, paragraph 1 | Audio Soccer is a football game you play by ear. It is made for blind and low-vision players, and everything you need to know comes to you as sound and speech. | فوتبال صوتی یک بازی فوتبال است که با گوش بازی می‌شود. این بازی برای بازیکنان نابینا و کم‌بینا ساخته شده است و هر چیزی که باید بدانید به شکل صدا و گفتار به شما می‌رسد. |
| full_description.txt, paragraph 2 | There are three levels. In the first you find the ball. In the second you take the ball to the goal and score. In the third you beat the keeper. Each level has three rounds. | بازی سه مرحله دارد. در مرحله اول توپ را پیدا می‌کنید. در مرحله دوم توپ را به دروازه می‌برید و گل می‌زنید. در مرحله سوم دروازه‌بان را شکست می‌دهید. هر مرحله سه دور دارد. |
| full_description.txt, paragraph 3 | You play on the phone's speaker, with the screen reader turned off. While the screen reader is off, the game speaks its own menus. Touch an option to hear it, and double-tap to choose it. If the screen reader is still on when a level is about to start, the game asks you to turn it off and waits. When you are done, you turn it back on yourself. | بازی را روی بلندگوی گوشی و با صفحه‌خوان خاموش بازی می‌کنید. تا وقتی صفحه‌خوان خاموش است، بازی خودش منوهایش را می‌خواند. گزینه را لمس کنید تا آن را بشنوید، و برای انتخاب دو بار ضربه بزنید. اگر هنگام شروع یک مرحله صفحه‌خوان هنوز روشن باشد، بازی از شما می‌خواهد آن را خاموش کنید و صبر می‌کند. وقتی بازی تمام شد، خودتان دوباره آن را روشن می‌کنید. |
| full_description.txt, paragraph 4 | To run, put a finger anywhere on the screen and drag it the way you want to go. Up the screen is toward the goal. Tap to kick. Tap with two fingers to hear where you are, and with three fingers to pause. The phone buzzes when the ball comes into reach and when you line up with the goal. | برای دویدن، انگشتتان را هر جای صفحه بگذارید و به سمتی که می‌خواهید بروید بکشید. بالای صفحه به سمت دروازه است. برای شوت ضربه بزنید. با دو انگشت ضربه بزنید تا بشنوید کجا هستید، و با سه انگشت تا بازی مکث کند. وقتی به توپ می‌رسید و وقتی با دروازه هم‌راستا می‌شوید، گوشی می‌لرزد. |
| full_description.txt, paragraph 5 | The game speaks English and Persian. It uses the speech engines installed on your phone, for example eSpeak, and in Settings you can choose which one it uses. If your phone has no speech engine, install one first. | بازی انگلیسی و فارسی حرف می‌زند. از موتورهای گفتاری استفاده می‌کند که روی گوشی شما نصب است، مثلاً eSpeak، و در تنظیمات می‌توانید یکی را انتخاب کنید. اگر گوشی شما موتور گفتار ندارد، اول یکی نصب کنید. |
| full_description.txt, paragraph 6 | The game needs no internet, shows no ads and does no tracking. It asks for one permission, to make the phone vibrate. It keeps two settings, your language and your speech engine, on your phone and nowhere else. | بازی اینترنت لازم ندارد، تبلیغ ندارد و کسی را ردیابی نمی‌کند. فقط یک مجوز می‌خواهد، برای لرزاندن گوشی. دو تنظیم شما، زبان و موتور گفتار، فقط روی گوشی خودتان نگه داشته می‌شود و جای دیگری نمی‌رود. |
| changelogs/1000.txt | First release. A football game played by ear for blind and low-vision players, in English and Persian. It has three levels: find the ball, score a goal and beat the keeper. You play with the screen reader turned off, while the game speaks its own menus with the speech engine you choose. | اولین نسخه. یک بازی فوتبال که با گوش بازی می‌شود، برای بازیکنان نابینا و کم‌بینا، به فارسی و انگلیسی. سه مرحله دارد: پیدا کردن توپ، گل زدن و شکست دادن دروازه‌بان. بازی را با صفحه‌خوان خاموش بازی می‌کنید و بازی خودش منوهایش را با موتور گفتاری که انتخاب می‌کنید می‌خواند. |
