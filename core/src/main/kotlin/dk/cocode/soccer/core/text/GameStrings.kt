package dk.cocode.soccer.core.text

// What the game says, from prototype/js/i18n/en.js. (UiStrings has what the app takes of the page text in en-ui.js and the
// phone test in en-phone.js.) A {slot} of the JS text is a
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

    // levels.js: the three levels are numbered 1 to 3
    fun levelTask(level: Int): String
    fun levelMiss(level: Int): String
    fun roundIntro(round: Int, task: String): String
    fun summaryDone(level: Int): String
    fun summaryTimes(head: String, list: String): String
    val summarySep: String

    // the sound tour
    val learnEnd: String
    val learnCalls: String
    val learnTarget: String
    val learnAhead: String
    val learnBehind: String
    val learnChimeNear: String
    val learnBoard: String
    val learnFeet: String
    val learnVoy: String
}
