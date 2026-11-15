package dk.cocode.soccer.core.text

// فارسی: everything the game says and shows, word for word from prototype/js/i18n/fa.js. Formal "شما" throughout and
// a ZWNJ (U+200C) where Persian needs it. Persian digits: digits() turns any ASCII digit that reaches a result into
// one, and a decimal point into ٫, so every member that takes a parameter goes through it.
internal object FaGame : GameStrings {
    // describe.js: clock positions, thirds, where am I, the goal guide
    override fun metres(n: Int) = digits("$n متر")
    override val clockFeet = "جلوی پایتان"
    override fun clockAt(hour: Int, metres: String) = digits("ساعت $hour، $metres")
    override val thirds = listOf(
        "یک‌سوم دفاعی",
        "یک‌سوم میانی",
        "یک‌سوم حمله",
    )
    override val sideLeft = "کنار دیواره چپ"
    override val sideRight = "کنار دیواره راست"
    override val sideCentre = "وسط"
    override val dirLeft = "چپ"
    override val dirRight = "راست"
    override fun wherePlace(third: String, side: String) = digits("$third، $side.")
    override fun whereBall(pos: String) = digits("توپ $pos.")
    override fun whereGuide(text: String) = digits("$text.")
    override fun whereKeeper(pos: String) = digits("دروازه‌بان $pos.")
    override fun whereRound(round: Int, total: Int, text: String) = digits("دور $round از $total. $text")
    override fun guideHead(m: String) = digits("دروازه، $m")
    override fun guideFar(head: String) = digits("$head، برای شوت زدن خیلی دور است")
    override fun guideAhead(head: String) = digits("$head، درست جلو")
    override fun guideSlightly(head: String, side: String) = digits("$head، کمی به $side")
    override fun guideMove(head: String, m: String, side: String) = digits("$head، $m به $side بروید")

    // spoken side calls (speakers mode) and ambient cues
    override val callAhead = "جلو"
    override val callLeft = "چپ"
    override val callRight = "راست"
    override val callBehind = "پشت سر"
    override val boardCallLeft = "دیواره چپ"
    override val boardCallRight = "دیواره راست"
    override val behind = "توپ پشت سر شماست"

    // game events
    override fun gotIt(time: String) = digits("پیدایش کردید. $time ثانیه.")
    override val gotBall = "توپ را گرفتید"
    override val goal = "گل!"
    override val saved = "دروازه‌بان گرفت!"
    override val post = "تیرک!"
    override fun wide(side: String) = digits("بیرون، سمت $side")
    override fun wideGoal(side: String) = digits("بیرون از دروازه، سمت $side")
    override val voy = "میام!"
    override val noKick = "برای شوت زدن باید توپ داشته باشید"
    override val timeUp = "وقت تمام شد."
    override val gaveUp = "دور را رها کردید."
    override val nextRound = "دور بعد چند لحظه دیگر شروع می‌شود."
    override val paused = "مکث. کلید فاصله ادامه می‌دهد."
    override val pausedBack = "مکث. کلید فاصله ادامه می‌دهد، Escape برای منو."
    override fun speed(rate: String) = digits("سرعت $rate")
    override fun speechSpeed(rate: String) = digits("سرعت گفتار $rate")
    override fun resume(level: Int, round: Int) = digits("ادامه مرحله $level، دور $round")

    // the status line of the page
    override val statusIdle = "یک مرحله را انتخاب کنید"
    override fun statusDone(level: Int) = digits("مرحله $level تمام شد")
    override val statusPaused = "مکث"
    override fun statusPlay(level: Int, name: String, round: Int, total: Int, time: String) =
        digits("مرحله $level، $name · دور $round از $total · $time ثانیه")

    // levels.js: the three levels are numbered 1 to 3
    override fun levelName(level: Int) = listOf("پیدا کردن توپ", "گل زدن", "شکست دادن دروازه‌بان")[level - 1]
    override fun levelTask(level: Int) = listOf("توپ را پیدا کنید.", "گل بزنید.", "دروازه‌بان را شکست بدهید.")[level - 1]
    override fun levelMiss(level: Int) = listOf("پیدا نشد", "گل نشد", "گل نشد")[level - 1]
    override fun roundIntro(round: Int, task: String) = digits("دور $round. $task")
    override fun extra(level: Int) = when (level) {
        2 -> listOf("کلید فاصله مستقیم به سمت جلوی زمین شوت می‌زند. با دروازه هم‌راستا شوید، تا حدود بیست متر، و شوت بزنید.")
        3 -> listOf(
            "کلید فاصله مستقیم به سمت جلوی زمین شوت می‌زند.",
            "دروازه‌بان خودش را با توپ هم‌خط می‌کند و بعد از شوت شما نمی‌تواند تکان بخورد. دروازه را کمی چپ یا راست خود قرار دهید، همان کلید جهت‌نما را نگه دارید و شوت بزنید.",
        )
        else -> emptyList()
    }
    override fun summaryDone(level: Int) = digits("مرحله $level تمام شد.")
    override fun summaryStopped(level: Int, n: Int, total: Int) =
        digits("مرحله $level بعد از $n دور از $total دور متوقف شد.")
    override fun summaryTimes(head: String, list: String) = digits("$head زمان‌ها: $list.")
    override val summarySep = "، "

    // the keyboard help
    override val help = listOf(
        "با کلید جهت‌نمای بالا به سمت دروازه می‌دوید و با پایین، به عقب.",
        "با کلیدهای جهت‌نمای چپ و راست به چپ و راست می‌دوید.",
        "دو کلید جهت‌نما را با هم نگه دارید تا مورب بدوید.",
        "کلید فاصله توپ را مستقیم به سمت جلوی زمین شوت می‌کند.",
        "هنگام شوت، چپ یا راست را نگه دارید تا شوت کج شود.",
        "W می‌گوید کجا هستید.",
        "G دور را رها می‌کند.",
        "کلیدهای کروشه، یا منفی و مثبت، سرعت گفتار را تغییر می‌دهند.",
        "Escape مکث می‌کند. کلید فاصله ادامه می‌دهد.",
        "H دوباره همین را می‌گوید.",
    )

    // the sound tour
    override val learnLeft = "چپ"
    override val learnRight = "راست"
    override val learnEnd = "صداها همین‌ها بود."
    override val learnCalls = "یک صدا می‌گوید هدف کجاست."
    override val learnTarget = "هدف توپ است، یا وقتی توپ با شماست، دروازه."
    override val learnAhead = "توپ تیک‌تیک می‌کند، جلوی شما."
    override val learnBehind = "همان توپ، پشت سر شما. صدایش گرفته‌تر است."
    override val learnRoll = "توپ از چپ شما به راست شما می‌غلتد."
    override val learnChime = "زنگ دروازه، جلو."
    override val learnChimeNear = "هرچه به دروازه نزدیک‌تر شوید، زنگ تندتر می‌شود."
    override val learnBoardL = "ضربه به دیواره چپ."
    override val learnBoardR = "و به دیواره راست."
    override val learnBoard = "ضربه به یک دیواره."
    override val learnFeet = "صدای قدم‌های شما."
    override val learnVoy = "دروازه‌بان داد می‌زند: میام!"

    // menu messages and page chrome
    override fun num(n: String) = digits(n)
    override val online = "آنلاین"
    override val voiceDefault = "صدای پیش‌فرض"
    override val voiceIs = "این صدای من است."
    override val noSound = "این مرورگر نمی‌تواند صدا پخش کند."
    override val touch = "این صفحه به کامپیوتر با صفحه‌کلید نیاز دارد. با گوشی، آزمایش گوشی را باز کنید: پیوندش درست زیر عنوان صفحه است."
    override fun copyBlocked(keys: String) = digits("کپی مسدود شد. متن انتخاب شده است؛ برای کپی $keys را بزنید.")
    override val copyDone = "کپی شد."
    override val clearConfirm = "برای پاک کردن همهٔ نتایج، تا ۱۵ ثانیه دیگر دوباره «پاک کردن نتایج» را بزنید."
    override val clearDone = "نتایج پاک شد."
    override val clearKept = "نتایج پاک شد. مرحلهٔ در حال بازی نگه داشته می‌شود."
    override val langSet = "فارسی"
    override val langFallback = "این دستگاه صدای فارسی آفلاین ندارد؛ از این به بعد صفحه‌خوان شما بازی را می‌خواند."
    override val canvasLeft = "سمت چپ شما · دیواره‌ها"
    override val canvasRight = "سمت راست شما · دیواره‌ها"
    override val canvasAttack = "حمله →"
    override val canvasYou = "شما"
    override val canvasKeeper = "دب"
}

private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
private val DECIMAL_POINT = Regex("(\\d)\\.(\\d)")

private fun digits(s: String): String {
    if (s.none { it in '0'..'9' }) return s // most results: their parts were turned already
    val withSeparator = DECIMAL_POINT.replace(s, "\$1٫\$2")
    return buildString(withSeparator.length) { for (c in withSeparator) append(if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c) }
}
