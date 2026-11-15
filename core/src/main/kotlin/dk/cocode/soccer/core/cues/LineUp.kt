package dk.cocode.soccer.core.cues

import dk.cocode.soccer.core.sim.AIM_HIGH
import dk.cocode.soccer.core.sim.AIM_LOW
import dk.cocode.soccer.core.sim.Event
import dk.cocode.soccer.core.sim.World
import dk.cocode.soccer.core.sim.aimedIn

private const val SLACK = 0.3 // m: once lined up, the ball must drift this far outside the band to count as off it

// A vibration pattern for the phone: alternating pause and buzz, in milliseconds.
enum class Buzz(val pattern: LongArray) {
    TAKE(longArrayOf(0, 40)), // the ball came into reach: the player took it
    LINED_UP(longArrayOf(0, 40, 80, 40)), // a straight kick would go in: the same test as the spoken side call ahead
}

// Vibration cues, from prototype/phone/js/buzz.js. Watches the world each frame and returns a buzz when something changes,
// never while it stays the same. A round starts with what it starts with: reset() says what, and nothing buzzes for it.
class LineUp {
    private var lined = false

    fun reset(w: World) {
        lined = w.hasBall && aimedIn(w.ball.y)
    }

    // The events are the frame's: a Take is the ball coming into reach. Lining up wins when both happen, as it says both.
    fun update(w: World, events: List<Event>): Buzz? {
        val y = w.ball.y
        val nowLined = w.hasBall && (if (lined) y >= AIM_LOW - SLACK && y <= AIM_HIGH + SLACK else aimedIn(y))
        val took = events.any { it is Event.Take }
        val buzz = when {
            nowLined && !lined -> Buzz.LINED_UP
            took -> Buzz.TAKE
            else -> null
        }
        lined = nowLined
        return buzz
    }
}
