package dk.cocode.soccer.core.text

// English: what the game says. From prototype/js/i18n/en.js, word for word, but for the records lines, which are new.
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

    // levels.js: the three levels are numbered 1 to 3
    override fun levelTask(level: Int) = listOf("Find the ball.", "Score a goal.", "Beat the keeper.")[level - 1]
    override fun levelMiss(level: Int) = listOf("not found", "not scored", "not scored")[level - 1]
    override fun roundIntro(round: Int, task: String) = "Round $round. $task"
    override fun summaryDone(level: Int) = "Level $level done."
    override fun summaryTimes(head: String, list: String) = "$head Times: $list."
    override val summarySep = ", "

    // records: new, not from the trial
    override fun bestTime(time: String) = "Best $time seconds."
    override fun newRecord(time: String) = "New record: $time seconds."
    override val allWon = "All three rounds won."

    // the sound tour
    override val learnEnd = "That is all the sounds."
    override val learnCalls = "A voice calls where the target is."
    override val learnTarget = "The target is the ball, or the goal when you have the ball."
    override val learnAhead = "Ball ticking, ahead of you."
    override val learnBehind = "The same ball, behind you. It sounds duller."
    override val learnChimeNear = "The goal chime beeps faster the closer you get to the goal."
    override val learnBoard = "A knock on a board."
    override val learnFeet = "Your footsteps."
    override val learnVoy = "The keeper shouts voy."
}
