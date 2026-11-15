package dk.cocode.soccer.core.cues

import dk.cocode.soccer.core.sim.Side
import dk.cocode.soccer.core.sim.World
import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.speech.Priority
import dk.cocode.soccer.core.text.GameStrings
import dk.cocode.soccer.core.text.guideText
import dk.cocode.soccer.core.text.thirdIndex
import dk.cocode.soccer.core.text.thirdName

private const val THIRD_HYSTERESIS = 0.5 // metres

// Spoken cues that come from watching the world: thirds, the ball passing behind, the goal guide, and the side calls and board
// calls. From prototype/js/cues.js. The words come from strings; every line goes to say, and the speech queue applies the
// gaps and waits the lines carry. isSaying asks that queue whether a line with a key is speaking or waiting; nowMs is the
// clock the side calls are spaced by, in milliseconds. A side call counts as heard only once the queue starts speaking it
// (Line.onSpoken). (The trial played a recorded clip for each word; the app speaks it.)
class Cues(
    private val strings: GameStrings,
    private val say: (Line) -> Unit,
    private val isSaying: (String) -> Boolean,
    private val nowMs: () -> Long,
) {
    // The board the player is near, and whether he is pressed against it (a bump).
    private data class AtBoard(val side: Side, val bumped: Boolean)

    private var third = 0
    private var ballWasAhead = true
    private var board: AtBoard? = null
    private var offered: SideWord? = null // the side call last handed to say
    private val caller = SideCaller()

    fun reset(w: World) {
        board = nearBoard(w.player)?.let { AtBoard(it, false) }
        third = thirdIndex(w.player.x)
        ballWasAhead = w.ball.x >= w.player.x
        caller.reset()
        offered = null
    }

    fun update(w: World) {
        val p = w.player
        val up = thirdIndex(p.x - THIRD_HYSTERESIS)
        val down = thirdIndex(p.x + THIRD_HYSTERESIS)
        if (up > third || down < third) {
            third = if (up > third) up else down
            say(Line(thirdName(third, strings)))
        }
        val ahead = w.ball.x - p.x
        if (ballWasAhead && ahead < -BEHIND && !w.hasBall) {
            // Once it is said, the side call behind would only repeat it. (Not if the queue drops it or holds it back for its gap.)
            say(Line(strings.behind, key = "behind", gapMs = 4000, onSpoken = { caller.spoke(SideWord.BEHIND, nowMs()) }))
        }
        if (ahead > 0) ballWasAhead = true else if (ahead < -BEHIND) ballWasAhead = false
        if (w.hasBall && thirdIndex(p.x) == 2) say(Line(guideText(p, strings, w.ball), key = "guide", gapMs = 6000))
        boardCall(w)
        sideCall(w)
    }

    // One short word when the target's side changes. The target is the ball, or the goal once you have it. A call that is not
    // started within its short wait is dropped: a late word would describe where you were, and it is offered again. The last
    // offer of this word may still be waiting, and a new line with the key replaces it and renews its wait, so it is not
    // handed over again every frame.
    private fun sideCall(w: World) {
        val word = caller.next(sideWord(w.player, w.ball, w.hasBall, caller.heard), nowMs()) ?: return
        if (word == offered && isSaying("side")) return
        offered = word
        val text = when (word) {
            SideWord.AHEAD -> strings.callAhead
            SideWord.LEFT -> strings.callLeft
            SideWord.RIGHT -> strings.callRight
            SideWord.BEHIND -> strings.callBehind
        }
        say(Line(text, Priority.LOW, cut = true, key = "side", waitMs = 800, onSpoken = { caller.spoke(word, nowMs()) }))
    }

    // The board brush and knock can't say which side on one speaker, so a short line names the board when you come within
    // BOARD_NEAR of it or bump it. Gap-limited, and a line not started within the short wait is dropped: a late "left board"
    // would describe where you were.
    private fun boardCall(w: World) {
        val now = nearBoard(w.player)?.let { AtBoard(it, w.playerTouching == it) }
        // Stepping off a bump while still near the board is not news.
        if (now != null && now != board && board != now.copy(bumped = true)) {
            val text = if (now.side == Side.LEFT) strings.boardCallLeft else strings.boardCallRight
            say(Line(text, Priority.LOW, key = "board", gapMs = 3000, waitMs = 600))
        }
        board = now
    }
}
