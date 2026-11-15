package dk.cocode.soccer.core.text

// فارسی: the same lines as EnUi. Formal "شما", plain everyday Persian, and a ZWNJ (U+200C) where Persian needs it. Lines
// taken word for word from prototype/js/i18n/fa-phone.js and fa.js; the new ones are in docs/persian-to-check.md.
internal object FaUi : UiStrings {
    override val greeting = "فوتبال صوتی. برای شنیدن گزینه‌ها صفحه را لمس کنید. برای انتخاب، دو بار ضربه بزنید."
    override val levelNames = listOf(
        "پیدا کردن توپ",
        "گل زدن",
        "شکست دادن دروازه‌بان",
    )
    override val learnSounds = "آشنایی با صداها"
    override val howToPlay = "راهنمای بازی"
    override val howToPlayLines = listOf(
        "انگشتتان را هر جای صفحه بگذارید و به سمتی که می‌خواهید بدوید بکشید.",
        "بالای صفحه به سمت دروازه است. انگشتتان را بردارید تا بایستید.",
        "برای شوت، با هر انگشتی ضربه بزنید، حتی وقتی دارید هدایت می‌کنید.",
        "با دو انگشت ضربه بزنید تا بشنوید کجا هستید.",
        "با سه انگشت ضربه بزنید تا بازی مکث کند.",
        "شوت مستقیم به سمت جلوی زمین می‌رود. با دروازه هم‌راستا شوید، تا حدود بیست متر، و شوت بزنید.",
    )
    override val settings = "تنظیمات"
    override val language = "زبان: فارسی"
    override fun voiceEngine(name: String) = "صدا: $name"
    override val resume = "ادامه"
    override val paused = "مکث"
    override val giveUp = "رها کردن این دور"
    override val backToMenu = "برگشت به منو"
    override val hearAgain = "شنیدن دوباره"
    override val handOver = "حالا صفحه‌خوان را خاموش کنید."
    override val dragIntro = listOf(
        "انگشتتان را هر جای صفحه بگذارید و به سمتی که می‌خواهید بدوید بکشید. بالای صفحه به سمت دروازه است. انگشتتان را بردارید تا بایستید.",
        "برای شوت، با هر انگشتی ضربه بزنید، حتی وقتی دارید هدایت می‌کنید.",
    )
    override val noEngine = "این گوشی موتور گفتار ندارد، برای همین بازی نمی‌تواند حرف بزند. یکی نصب کنید، مثلاً eSpeak، و بازی را دوباره باز کنید."
    override val englishOnly = "این صدا فارسی حرف نمی‌زند، برای همین بازی انگلیسی حرف می‌زند."
}
