package dk.cocode.soccer.core.text

// Everything the game says and shows, one member per leaf of prototype/js/i18n/en.js. (The page text in en-ui.js and
// the phone test in en-phone.js are not here: UiStrings has what the app takes of them.) A {slot} of the JS text is a
// parameter: Int where the slot held a number, String where the caller has already formatted it. The Persian
// implementation turns every ASCII digit that reaches a result into a Persian one, as fa.js digits() does.
interface GameStrings {
    fun metres(n: Int): String // "1 metre", "3 metres"

    // describe.js: clock positions, thirds, where am I, the goal guide
    val clockFeet: String
    fun clockAt(hour: Int, metres: String): String
    val thirds: List<String>
    val sideLeft: String
    val sideRight: String
    val sideCentre: String
    val dirLeft: String
    val dirRight: String
    fun wherePlace(third: String, side: String): String
    fun whereBall(pos: String): String
    fun whereGuide(text: String): String
    fun whereKeeper(pos: String): String
    fun whereRound(round: Int, total: Int, text: String): String
    fun guideHead(m: String): String
    fun guideFar(head: String): String
    fun guideAhead(head: String): String
    fun guideSlightly(head: String, side: String): String
    fun guideMove(head: String, m: String, side: String): String

    // spoken side calls (speakers mode) and ambient cues
    val callAhead: String
    val callLeft: String
    val callRight: String
    val callBehind: String
    val boardCallLeft: String
    val boardCallRight: String
    val behind: String

    // game events
    fun gotIt(time: String): String
    val gotBall: String
    val goal: String
    val saved: String
    val post: String
    fun wide(side: String): String
    fun wideGoal(side: String): String
    val voy: String
    val noKick: String
    val timeUp: String
    val gaveUp: String
    val nextRound: String
    val paused: String
    val pausedBack: String
    fun speed(rate: String): String
    fun speechSpeed(rate: String): String
    fun resume(level: Int, round: Int): String

    // the status line of the page
    val statusIdle: String
    fun statusDone(level: Int): String
    val statusPaused: String
    fun statusPlay(level: Int, name: String, round: Int, total: Int, time: String): String

    // levels.js: the three levels are numbered 1 to 3
    fun levelName(level: Int): String
    fun levelTask(level: Int): String
    fun levelMiss(level: Int): String
    fun roundIntro(round: Int, task: String): String
    fun extra(level: Int): List<String> // the extra lines of round 1; none for level 1
    fun summaryDone(level: Int): String
    fun summaryStopped(level: Int, n: Int, total: Int): String
    fun summaryTimes(head: String, list: String): String
    val summarySep: String

    // the keyboard help
    val help: List<String>

    // the sound tour
    val learnLeft: String
    val learnRight: String
    val learnEnd: String
    val learnCalls: String
    val learnTarget: String
    val learnAhead: String
    val learnBehind: String
    val learnRoll: String
    val learnChime: String
    val learnChimeNear: String
    val learnBoardL: String
    val learnBoardR: String
    val learnBoard: String
    val learnFeet: String
    val learnVoy: String

    // menu messages and page chrome
    fun num(n: String): String
    val online: String
    val voiceDefault: String
    val voiceIs: String
    val noSound: String
    val touch: String
    fun copyBlocked(keys: String): String
    val copyDone: String
    val clearConfirm: String
    val clearDone: String
    val clearKept: String
    val langSet: String
    val langFallback: String
    val canvasLeft: String
    val canvasRight: String
    val canvasAttack: String
    val canvasYou: String
    val canvasKeeper: String
}
