package dk.cocode.soccer.core.cues

import dk.cocode.soccer.core.sim.BOARD_NEAR
import dk.cocode.soccer.core.sim.GoalSide
import dk.cocode.soccer.core.sim.PITCH_W
import dk.cocode.soccer.core.sim.Side
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.clamp
import dk.cocode.soccer.core.sim.goalSide
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Pure logic for speakers, where nothing is panned: distance is carried by volume and by how fast the beacon beeps, and a
// word names the side the target is on. From prototype/js/speakers.js. No audio, no speech.

private const val REF = 2.0
private const val ROLLOFF = 0.3
private const val FLOOR = 0.3

// Gain of a sound at distance d (m): 1 within REF m, then a gentle inverse curve that stops at FLOOR (about 18 m). Far sounds
// stay audible on a phone speaker at a comfortable volume; the tick and the beacon getting faster as you close in carry the
// rest of the distance.
fun distanceGain(d: Double): Double = max(FLOOR, REF / (REF + ROLLOFF * (max(d, REF) - REF)))

// Seconds between goal chimes while you have the ball: 2 s from 30 m, 0.5 s within 6 m, linear between, so closing in on the
// goal speeds the beeps up (like the still ball's tick).
fun beaconEvery(d: Double): Double = clamp(0.5 + (d - 6) * 1.5 / 24, 0.5, 2.0)

private const val LATERAL = 1.5 // once ahead is heard, the ball stays ahead within this many metres to either side,
private const val NEAR_LATERAL = 0.5 // narrowing to this close by: under DRIBBLE_DIST, so obeying the calls takes it
private const val ON_LINE = 0.3 // after any other word, ahead only once you are this close to the ball's line
internal const val BEHIND = 1.0 // the ball counts as behind when more than this far back (less when close)

// The words of a side call. The text of each comes from GameStrings.
internal enum class SideWord { AHEAD, LEFT, RIGHT, BEHIND }

// Half-width of the ahead band for a ball this many metres in front or behind: LATERAL from 6 m out, NEAR_LATERAL within 2 m,
// linear between. Wide far away so the word does not flap while you run; narrow close by, since a ball 1.2 m to the side
// called ahead is never taken (DRIBBLE_DIST is 0.8 m).
private fun band(ahead: Double) = clamp(NEAR_LATERAL + (abs(ahead) - 2) / 4, NEAR_LATERAL, LATERAL)

// The word for where the target is. heard is the last word the player heard (SideCaller.heard).
// The ball (you don't have it): behind when more than BEHIND m back (the band's width when close), else by the side it is
// on. Hysteresis, because the player keeps going the way the last word said: after a side word ahead comes only within
// ON_LINE of the ball's line, and once heard it holds within the band (never under NEAR_LATERAL). Running ahead keeps a still
// ball's side offset, so it stays under ON_LINE, the word does not flip back, and the ball is taken (DRIBBLE_DIST is 0.8 m).
// The goal (you have the ball): judged from the ball, where a kick leaves, by goalSide, the same test the goal guide uses:
// ahead when a straight kick lines up with the goal, at any distance. Ahead is the word that stops a sideways run, so it
// must come beyond SHOT_RANGE too (level 2 and 3 rounds start there); whether a kick reaches is the where answer's job ("too
// far to shoot").
internal fun sideWord(player: Vec, ball: Vec, hasBall: Boolean, heard: SideWord? = null): SideWord {
    if (hasBall) {
        return when (goalSide(ball.y)) {
            GoalSide.AHEAD -> SideWord.AHEAD
            GoalSide.LEFT -> SideWord.LEFT
            GoalSide.RIGHT -> SideWord.RIGHT
        }
    }
    val ahead = ball.x - player.x
    val right = ball.y - player.y
    val half = band(ahead)
    if (ahead < -min(BEHIND, half)) return SideWord.BEHIND
    return when {
        abs(right) <= (if (heard == SideWord.AHEAD) half else ON_LINE) -> SideWord.AHEAD
        right < 0 -> SideWord.LEFT
        else -> SideWord.RIGHT
    }
}

// The board the player is within BOARD_NEAR of (the brush zone, which a bump is inside): LEFT, RIGHT or null.
internal fun nearBoard(p: Vec): Side? = when {
    p.y < BOARD_NEAR -> Side.LEFT
    p.y > PITCH_W - BOARD_NEAR -> Side.RIGHT
    else -> null
}

internal const val SIDE_GAP = 1500L // ms between side calls

// Decides when a side call may be spoken: not within SIDE_GAP of the last one, never the same word twice in a row. next(word, t)
// returns the word to say or null; spoke(word, t) records a call that was spoken; heard is the last word spoken. Ahead skips
// the gap: it is the word to stop stepping sideways, and the player keeps going the way the last word said, so 1.5 s late he
// is 6 m past the line.
internal class SideCaller {
    var heard: SideWord? = null
        private set
    private var at = 0L // when heard was spoken; meaningless while nothing has been heard

    fun reset() {
        heard = null
    }

    fun next(word: SideWord, t: Long): SideWord? {
        val tooSoon = word != SideWord.AHEAD && heard != null && t - at < SIDE_GAP
        return if (word == heard || tooSoon) null else word
    }

    fun spoke(word: SideWord, t: Long) {
        heard = word
        at = t
    }
}
