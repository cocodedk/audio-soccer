package dk.cocode.soccer.core.sim

import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

// Shared test helpers, from prototype/test/helpers.mjs.

const val DT = 1.0 / 120

fun close(a: Double, b: Double, eps: Double = 1e-9) = assertTrue(abs(a - b) < eps, "$a != $b")

// The player at (10, 10), where the cue and sound tests set their rounds up, and a ball.
fun ballWorld(ball: Vec, hasBall: Boolean = false, player: Vec = Vec(10.0, 10.0)): World = createWorld(player, ball, hasBall)

inline fun <reified T : Event> List<Event>.has() = any { it is T }

inline fun <reified T : Event> List<Event>.ofType(): List<T> = filterIsInstance<T>()

// Step a world for secs seconds at 120 Hz with a fixed direction; returns every event.
fun run(w: World, secs: Double, dir: Vec = Vec.ZERO): List<Event> {
    val events = mutableListOf<Event>()
    var t = 0.0
    while (t < secs) {
        events += step(w, DT, dir)
        t += DT
    }
    return events
}

private val ARROWS = mapOf(
    "Up" to Vec(1.0, 0.0), "Down" to Vec(-1.0, 0.0), "Left" to Vec(0.0, -1.0), "Right" to Vec(0.0, 1.0),
)

// Held arrow keys to a unit direction, as arrowsToDir in world.js. The app has no keyboard, so this
// lives with the tests that port the trial's key-driven cases.
fun arrows(vararg keys: String): Vec {
    var x = 0.0
    var y = 0.0
    for (k in keys) ARROWS[k]?.let { x += it.x; y += it.y }
    val len = hypot(x, y)
    return if (len != 0.0) Vec(x / len, y / len) else Vec.ZERO
}

// A small seeded random generator (mulberry32), so simulated rates are the same on every run. Only nextDouble
// is the trial's sequence; the other draws of Random are derived from it.
fun seeded(seed: Int): Random {
    var a = seed
    return object : Random() {
        override fun nextDouble(): Double {
            a += 0x6D2B79F5
            var t = (a xor (a ushr 15)) * (1 or a)
            t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
            return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
        }

        override fun nextBits(bitCount: Int): Int =
            if (bitCount == 0) 0 else (nextDouble() * 4294967296.0).toLong().toInt() ushr (32 - bitCount)
    }
}
