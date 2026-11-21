# Persian to check before release

Every Persian string the Android app has that the browser trial did not already have, next to its English. They are in
`core/src/main/kotlin/dk/cocode/soccer/core/text/FaUi.kt`, except the three records lines, which are in `FaGame.kt`. Please read them and say what to change.

Written for a screen reader user: formal "شما", the same words as the trial (صفحه‌خوان، شوت بزنید، دروازه‌بان، مکث),
and ZWNJ where Persian needs it. `eSpeak` stays in Latin letters, as a product name.

## New

| Member | English | Persian |
|---|---|---|
| greeting | Audio Soccer. Hold a finger on the screen to hear the options, or swipe to move through them. Tap to hear the option again, double-tap to choose. | فوتبال صوتی. برای شنیدن گزینه‌ها انگشتتان را روی صفحه نگه دارید، یا انگشت را بکشید تا بین آن‌ها جابه‌جا شوید. برای شنیدن دوباره‌ی گزینه یک بار ضربه بزنید، و برای انتخاب دو بار. |
| howToPlay | How to play | راهنمای بازی |
| howToPlayLines\[4] | Tap with three fingers to pause. | با سه انگشت ضربه بزنید تا بازی مکث کند. |
| settings | Settings | تنظیمات |
| language | Language: English | زبان: فارسی |
| voiceEngine(name) | Voice: {name} | صدا: {name} |
| resume | Resume | ادامه |
| giveUp | Give up this round | رها کردن این دور |
| noEngine | This phone has no speech engine, so the game cannot speak. Install one, for example eSpeak. After installing it, open the speech engine's own app once, so it can get its voices ready. Then open the game again. | این گوشی موتور گفتار ندارد، برای همین بازی نمی‌تواند حرف بزند. یکی نصب کنید، مثلاً eSpeak. بعد از نصب، یک بار برنامه خود موتور گفتار را باز کنید تا صدایش آماده شود. بعد بازی را دوباره باز کنید. |
| englishOnly | This voice cannot speak Persian, so the game speaks English. | این صدا فارسی حرف نمی‌زند، برای همین بازی انگلیسی حرف می‌زند. |
| bestTime(time), in FaGame.kt | Best {time} seconds. (after a level's name on the menu) | بهترین زمان {time} ثانیه. |
| newRecord(time), in FaGame.kt | New record: {time} seconds. (in the level summary) | رکورد جدید: {time} ثانیه. |
| allWon, in FaGame.kt | All three rounds won. (in the summary, and after a level's name once it happened) | هر سه دور را بردید. |

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
short description may be no longer than 80 characters. `eSpeak NG` stays in Latin letters. Please read them and say what to change.

| File | English | Persian |
|---|---|---|
| title.txt | Audio Soccer | فوتبال صوتی |
| short_description.txt | Play football by ear, for blind and low-vision players. No internet, no ads. | با گوش فوتبال بازی کنید، برای نابینایان و کم‌بینایان. بدون اینترنت و تبلیغ |
| full_description.txt, paragraph 1 | Audio Soccer is a football game you play by ear, made for blind and low-vision players. | فوتبال صوتی یک بازی فوتبال است که با گوش بازی می‌شود و برای بازیکنان نابینا و کم‌بینا ساخته شده است. |
| full_description.txt, paragraph 2 | There are three levels. In the first you find the ball. In the second you take the ball to the goal and score. In the third you beat the keeper. Each level has three rounds. A fanfare plays for every round you win. Each level remembers your best time and whether you won all three rounds: the level's menu option tells you, and the summary at the end of the level tells you when you set a new record. | بازی سه مرحله دارد. در مرحله اول توپ را پیدا می‌کنید. در مرحله دوم توپ را به دروازه می‌برید و گل می‌زنید. در مرحله سوم دروازه‌بان را شکست می‌دهید. هر مرحله سه دور دارد. برای هر دوری که می‌برید یک آهنگ پیروزی پخش می‌شود. هر مرحله بهترین زمان شما را به خاطر می‌سپارد، و اینکه هر سه دورش را برده‌اید یا نه: گزینه‌ی همان مرحله در منو این را می‌گوید، و خلاصه‌ی پایان مرحله رکورد جدید را اعلام می‌کند. |
| full_description.txt, paragraph 3 | You play on the phone's speaker, with the screen reader turned off. While the screen reader is on, the menus are ordinary buttons it reads, so you can set things up first. If the screen reader is still on when a level is about to start, the game asks you to turn it off and waits. When you are done, you turn it back on yourself. | بازی را روی بلندگوی گوشی و با صفحه‌خوان خاموش بازی می‌کنید. تا وقتی صفحه‌خوان روشن است، منوها دکمه‌های معمولی هستند که صفحه‌خوان آن‌ها را می‌خواند، پس می‌توانید اول تنظیمات را انجام دهید. اگر هنگام شروع یک مرحله صفحه‌خوان هنوز روشن باشد، بازی از شما می‌خواهد آن را خاموش کنید و صبر می‌کند. وقتی بازی تمام شد، خودتان دوباره آن را روشن می‌کنید. |
| full_description.txt, paragraph 4 | While the screen reader is off, the game speaks its own menus. Hold a finger on the screen to hear an option, swipe to move from one option to the next (quickly with one finger, or at any speed with two), tap once to hear the current option again, and double-tap to choose it. | تا وقتی صفحه‌خوان خاموش است، بازی خودش منوهایش را می‌خواند. انگشتتان را روی صفحه نگه دارید تا گزینه را بشنوید، برای رفتن از یک گزینه به گزینه‌ی بعد انگشت را بکشید (با یک انگشت سریع، یا با دو انگشت با هر سرعتی)، برای شنیدن دوباره‌ی گزینه‌ی فعلی یک بار ضربه بزنید، و برای انتخاب دو بار ضربه بزنید. |
| full_description.txt, paragraph 5 | To run, put a finger anywhere on the screen and drag it the way you want to go. Up the screen is toward the goal. Tap to kick. Tap with two fingers to hear where you are, and with three fingers to pause. The phone vibrates when the ball comes into reach and when you line up with the goal. | برای دویدن، انگشتتان را هر جای صفحه بگذارید و به سمتی که می‌خواهید بروید بکشید. بالای صفحه به سمت دروازه است. برای شوت ضربه بزنید. با دو انگشت ضربه بزنید تا بشنوید کجا هستید، و با سه انگشت تا بازی مکث کند. وقتی به توپ می‌رسید و وقتی با دروازه هم‌راستا می‌شوید، گوشی می‌لرزد. |
| full_description.txt, paragraph 6 | If you can see some of the screen, the menus are white text on black with the option in focus in yellow, and the pitch is drawn in bright colours on black: you in cyan, the ball in orange, the keeper in pink and the goal in yellow. | اگر بخشی از صفحه را می‌بینید، متن منوها سفید روی زمینه‌ی سیاه است و گزینه‌ای که روی آن هستید زرد است، و زمین بازی با رنگ‌های روشن روی زمینه‌ی سیاه کشیده شده است: خودتان فیروزه‌ای، توپ نارنجی، دروازه‌بان صورتی و دروازه زرد. |
| full_description.txt, paragraph 7 | The game speaks English and Persian. It uses the speech engines installed on your phone, for example eSpeak NG, and in Settings you choose which one it uses. If your phone has no speech engine, install one first. | بازی انگلیسی و فارسی حرف می‌زند. از موتورهای گفتاری استفاده می‌کند که روی گوشی شما نصب است، مثلاً eSpeak NG، و در تنظیمات می‌توانید یکی را انتخاب کنید. اگر گوشی شما موتور گفتار ندارد، اول یکی نصب کنید. |
| full_description.txt, paragraph 8 | The game has no internet permission, no ads, no tracking and no analytics. It uses two permissions: one to make the phone vibrate, and one added by an Android library that only apps signed with the same key can hold. Your settings and records stay on your phone. They are not backed up, and they are deleted when you uninstall the app or clear its data. | بازی مجوز اینترنت ندارد، و تبلیغ، ردیابی و تحلیل آماری هم ندارد. دو مجوز دارد: یکی برای لرزاندن گوشی، و یکی که یک کتابخانه‌ی اندروید اضافه می‌کند و فقط برنامه‌هایی که با همان کلید امضا شده‌اند می‌توانند آن را داشته باشند. تنظیمات و رکوردهای شما فقط روی گوشی خودتان می‌ماند. از آن‌ها نسخه‌ی پشتیبان گرفته نمی‌شود، و وقتی برنامه را حذف کنید یا داده‌هایش را پاک کنید، پاک می‌شوند. |
| full_description.txt, paragraph 9 | It needs Android 7.0 or newer. | به اندروید ۷ یا جدیدتر نیاز دارد. |
| changelogs/1000.txt | First release (0.1.0). A football game you play by ear, for blind and low-vision players, in English and Persian. It has three levels: find the ball, score a goal and beat the keeper. You play with the screen reader turned off, while the game speaks its own menus with the speech engine you choose. A fanfare plays for every round you win, and each level remembers your best time and whether you won all three rounds. | اولین نسخه (0.1.0). یک بازی فوتبال که با گوش بازی می‌شود، برای بازیکنان نابینا و کم‌بینا، به فارسی و انگلیسی. سه مرحله دارد: پیدا کردن توپ، گل زدن و شکست دادن دروازه‌بان. بازی را با صفحه‌خوان خاموش بازی می‌کنید و بازی خودش منوهایش را با موتور گفتاری که انتخاب می‌کنید می‌خواند. برای هر دوری که می‌برید یک آهنگ پیروزی پخش می‌شود، و هر مرحله بهترین زمان شما را به خاطر می‌سپارد و اینکه هر سه دورش را برده‌اید یا نه. |

## Site and trial

The two Persian site pages are new from start to finish: `site/fa/app/index.html` (about the app) and
`site/fa/privacy/index.html` (the privacy policy, the Persian twin of `site/privacy/index.html`). Merging to main publishes them at
once, so please read them before that. The Persian footer under the trial is in `site/foot.html`.

The trial's own new lines, in `prototype/js/i18n/`. The trial writes the ezafe with «ٔ» (برنامهٔ), the app, the store texts
and the site with «‌ی» (برنامه‌ی); say if one of the two should be used everywhere.

| Key | English | Persian |
|---|---|---|
| ui.appLink (fa-ui.js) | The Android app is coming. Read about it. | برنامهٔ اندروید در راه است. دربارهٔ آن بخوانید. |
| ui.skip (fa-ui.js) | Skip to the levels | پرش به مرحله‌ها |
| ui.canvasLabel (fa-ui.js), reworded | Top-down picture of the pitch, for low-vision players and a sighted friend. You attack to the right; your left is the top board. | تصویر زمین از بالا، برای بازیکنان کم‌بینا و یک دوست بینا. شما به سمت راست حمله می‌کنید؛ سمت چپ شما، دیواره بالایی است. |
| ui.back (fa-phone.js) | Back to the Audio Soccer trial | بازگشت به آزمایش فوتبال صوتی |
