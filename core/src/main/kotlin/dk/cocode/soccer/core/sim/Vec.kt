package dk.cocode.soccer.core.sim

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// A point or direction on the pitch: x toward the goal, y from the player's left to right.
data class Vec(val x: Double, val y: Double) {
    companion object {
        val ZERO = Vec(0.0, 0.0)
    }
}

fun dist(a: Vec, b: Vec): Double = hypot(a.x - b.x, a.y - b.y)

fun clamp(v: Double, a: Double, b: Double): Double = max(a, min(v, b))

// JavaScript's Math.round: halves go up. The trial's spoken numbers rely on it.
fun roundHalfUp(v: Double): Double = floor(v + 0.5)

// One decimal place, rounded half up on the scaled value (String.format would follow the phone's locale). Shared by
// the kick report and the spoken times. This is not JavaScript's toFixed(1) on binary ties: 0.15 and 2.05 sit just
// below the tie and toFixed rounds them down, while v * 10 lands exactly on the tie here and rounds up.
internal fun tenths(v: Double): String {
    val n = roundHalfUp(v * 10).toLong()
    return "${n / 10}.${n % 10}"
}

private val D = sqrt(0.5)

// The 8 directions the keyboard gave in the trial, counter-clockwise from straight up the pitch, with
// exact zeros so a straight run kicks straight (kickDir angles a kick by the sign of y).
private val DIRS = listOf(
    Vec(1.0, 0.0), Vec(D, D), Vec(0.0, 1.0), Vec(-D, D), Vec(-1.0, 0.0), Vec(-D, -D), Vec(0.0, -1.0), Vec(D, -D),
)

// A pitch-space vector (forward, right) snapped to the nearest of the 8 directions; none for a zero vector.
fun snap8(forward: Double, right: Double): Vec {
    if (forward == 0.0 && right == 0.0) return Vec.ZERO
    val i = roundHalfUp(atan2(right, forward) / (PI / 4)).toInt()
    return DIRS[i.mod(8)]
}
