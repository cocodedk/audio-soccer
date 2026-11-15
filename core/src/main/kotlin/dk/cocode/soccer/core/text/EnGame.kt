package dk.cocode.soccer.core.text

// English: everything the game says and shows. From prototype/js/i18n/en.js, word for word.
internal object EnGame : GameStrings {
    // describe.js: clock positions, thirds, where am I, the goal guide
    override fun metres(n: Int) = "$n metre${if (n == 1) "" else "s"}"
    override val clockFeet = "at your feet"
    override fun clockAt(hour: Int, metres: String) = "$hour o'clock, $metres"
    override val thirds = listOf(
        "Defending third",
        "Middle third",
        "Attacking third",
    )
    override val sideLeft = "by the left board"
    override val sideRight = "by the right board"
    override val sideCentre = "centre"
    override val dirLeft = "left"
    override val dirRight = "right"
    override fun wherePlace(third: String, side: String) = "$third, $side."
    override fun whereBall(pos: String) = "Ball $pos."
    override fun whereGuide(text: String) = "$text."
    override fun whereKeeper(pos: String) = "Keeper $pos."
    override fun whereRound(round: Int, total: Int, text: String) = "Round $round of $total. $text"
    override fun guideHead(m: String) = "Goal, $m"
    override fun guideFar(head: String) = "$head, too far to shoot"
    override fun guideAhead(head: String) = "$head, straight ahead"
    override fun guideSlightly(head: String, side: String) = "$head, slightly $side"
    override fun guideMove(head: String, m: String, side: String) = "$head, move $m $side"

    // spoken side calls (speakers mode) and ambient cues
    override val callAhead = "ahead"
    override val callLeft = "left"
    override val callRight = "right"
    override val callBehind = "behind"
    override val boardCallLeft = "left board"
    override val boardCallRight = "right board"
    override val behind = "Ball behind you"

    // game events
    override fun gotIt(time: String) = "Got it. $time seconds."
    override val gotBall = "Got the ball"
    override val goal = "Goal!"
    override val saved = "Saved!"
    override val post = "Post!"
    override fun wide(side: String) = "Wide $side"
    override fun wideGoal(side: String) = "Wide $side of the goal"
    override val voy = "Voy!"
    override val noKick = "You need the ball to kick"
    override val timeUp = "Time is up."
    override val gaveUp = "Round given up."
    override val nextRound = "Next round in a moment."
    override val paused = "Paused. Space resumes."
    override val pausedBack = "Paused. Space resumes, Escape for the menu."
    override fun speed(rate: String) = "Speed $rate"
    override fun speechSpeed(rate: String) = "Speech speed $rate"
    override fun resume(level: Int, round: Int) = "Resume level $level, round $round"

    // the status line of the page
    override val statusIdle = "Choose a level to start"
    override fun statusDone(level: Int) = "Level $level done"
    override val statusPaused = "Paused"
    override fun statusPlay(level: Int, name: String, round: Int, total: Int, time: String) =
        "Level $level, $name · Round $round of $total · $time s"

    // levels.js: the three levels are numbered 1 to 3
    override fun levelName(level: Int) = listOf("Find the ball", "Score", "Beat the keeper")[level - 1]
    override fun levelTask(level: Int) = listOf("Find the ball.", "Score a goal.", "Beat the keeper.")[level - 1]
    override fun levelMiss(level: Int) = listOf("not found", "not scored", "not scored")[level - 1]
    override fun roundIntro(round: Int, task: String) = "Round $round. $task"
    override fun extra(level: Int) = when (level) {
        2 -> listOf("Space kicks straight up the pitch. Line up with the goal, within about twenty metres, and kick.")
        3 -> listOf(
            "Space kicks straight up the pitch.",
            "The keeper sets himself on the ball and can't move once you kick. Get the goal slightly to your left or right, hold that arrow, and kick.",
        )
        else -> emptyList()
    }
    override fun summaryDone(level: Int) = "Level $level done."
    override fun summaryStopped(level: Int, n: Int, total: Int) = "Level $level stopped after $n of $total rounds."
    override fun summaryTimes(head: String, list: String) = "$head Times: $list."
    override val summarySep = ", "

    // the keyboard help
    override val help = listOf(
        "Arrow up runs toward the goal. Arrow down runs back.",
        "Left and right arrows run to your left and right.",
        "Hold two arrows to run diagonally.",
        "Space kicks the ball straight up the pitch.",
        "Hold left or right while you kick to angle the shot.",
        "W tells you where you are.",
        "G gives up the round.",
        "The bracket keys, or minus and plus, change the speech speed.",
        "Escape pauses. Space resumes.",
        "H says this again.",
    )

    // the sound tour
    override val learnLeft = "Left"
    override val learnRight = "Right"
    override val learnEnd = "That is all the sounds."
    override val learnCalls = "A voice calls where the target is."
    override val learnTarget = "The target is the ball, or the goal when you have the ball."
    override val learnAhead = "Ball ticking, ahead of you."
    override val learnBehind = "The same ball, behind you. It sounds duller."
    override val learnRoll = "Ball rolling from your left to your right."
    override val learnChime = "The goal chime, ahead."
    override val learnChimeNear = "The goal chime beeps faster the closer you get to the goal."
    override val learnBoardL = "A knock on the left board."
    override val learnBoardR = "And on the right board."
    override val learnBoard = "A knock on a board."
    override val learnFeet = "Your footsteps."
    override val learnVoy = "The keeper shouts voy."

    // menu messages and page chrome
    override fun num(n: String) = n
    override val online = "online"
    override val voiceDefault = "Default voice"
    override val voiceIs = "This is the voice."
    override val noSound = "This browser cannot play sound."
    override val touch = "This page needs a computer with a keyboard. On a phone, open the phone test: the link is just below the heading."
    override fun copyBlocked(keys: String) = "Copying was blocked. The text is selected: press $keys to copy it."
    override val copyDone = "Copied."
    override val clearConfirm = "Press Clear results again within 15 seconds to delete all results."
    override val clearDone = "Results cleared."
    override val clearKept = "Results cleared. The level in progress is kept."
    override val langSet = "English"
    override val langFallback = "There is no Persian voice on this device. From now on your screen reader speaks the game."
    override val canvasLeft = "YOUR LEFT · BOARDS"
    override val canvasRight = "YOUR RIGHT · BOARDS"
    override val canvasAttack = "ATTACKING →"
    override val canvasYou = "YOU"
    override val canvasKeeper = "GK"
}
