package dk.cocode.soccer.core.text

// فارسی: what the game says, word for word from prototype/js/i18n/fa.js. Formal "شما" throughout and
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

    // levels.js: the three levels are numbered 1 to 3
    override fun levelTask(level: Int) = listOf("توپ را پیدا کنید.", "گل بزنید.", "دروازه‌بان را شکست بدهید.")[level - 1]
    override fun levelMiss(level: Int) = listOf("پیدا نشد", "گل نشد", "گل نشد")[level - 1]
    override fun roundIntro(round: Int, task: String) = digits("دور $round. $task")
    override fun summaryDone(level: Int) = digits("مرحله $level تمام شد.")
    override fun summaryTimes(head: String, list: String) = digits("$head زمان‌ها: $list.")
    override val summarySep = "، "

    // the sound tour
    override val learnEnd = "صداها همین‌ها بود."
    override val learnCalls = "یک صدا می‌گوید هدف کجاست."
    override val learnTarget = "هدف توپ است، یا وقتی توپ با شماست، دروازه."
    override val learnAhead = "توپ تیک‌تیک می‌کند، جلوی شما."
    override val learnBehind = "همان توپ، پشت سر شما. صدایش گرفته‌تر است."
    override val learnChimeNear = "هرچه به دروازه نزدیک‌تر شوید، زنگ تندتر می‌شود."
    override val learnBoard = "ضربه به یک دیواره."
    override val learnFeet = "صدای قدم‌های شما."
    override val learnVoy = "دروازه‌بان داد می‌زند: میام!"
}

private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
private val DECIMAL_POINT = Regex("(\\d)\\.(\\d)")

private fun digits(s: String): String {
    if (s.none { it in '0'..'9' }) return s // most results: their parts were turned already
    val withSeparator = DECIMAL_POINT.replace(s, "\$1٫\$2")
    return buildString(withSeparator.length) { for (c in withSeparator) append(if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c) }
}
