package dk.cocode.soccer.core.input

import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.snap8

enum class PlayInput { KICK, WHERE, PAUSE }

// The play rules of prototype/phone/js/gestures.js. One finger steers by dragging: the first to move DEAD_DP becomes
// the steering finger, dir is its drag from where it landed (screen up runs toward the goal, right runs to the player's
// right) and lifting it stops the run. Other fingers can tap meanwhile: a finger joins the tap of the fingers that
// landed within CHORD_MS before it, and the tap is reported once all of them are up, so two fingers together are one
// WHERE, never two KICKs. The steering finger never counts as a tap.
//
// Each call returns at most one input. A tap whose last finger timed out is returned by the next on() or tick() (call
// tick every frame), and if two taps complete at once the second is returned by the call after, so none is lost.
// CANCEL ends only the finger it names: after a lost gesture, send one for each finger down or call reset().
class PlayGestures {
    private class Chord(val t0: Long) {
        var waiting = 0
        var taps = 0
    }

    private class Finger(val id: Int, val x0: Float, val y0: Float, val t0: Long, var chord: Chord?) {
        var x = x0
        var y = y0
    }

    private val fingers = ArrayList<Finger>()
    private var steering: Finger? = null
    private var lastChord: Chord? = null

    // The steering direction now, or Vec.ZERO: no steering finger, or it stands inside the dead zone.
    val dir: Vec
        get() {
            val f = steering ?: return Vec.ZERO
            val dx = f.x - f.x0
            val dy = f.y - f.y0
            return if (pastDead(dx, dy)) snap8(-dy.toDouble(), dx.toDouble()) else Vec.ZERO
        }

    fun on(phase: Phase, t: Touch): PlayInput? = when (phase) {
        Phase.DOWN -> down(t)
        Phase.MOVE -> move(t)
        Phase.UP -> up(t)
        Phase.CANCEL -> find(t.id)?.let { release(it, false) }
    }

    // A finger held still past the tap time is no longer a tap candidate.
    fun tick(nowMs: Long): PlayInput? = expire(nowMs)

    fun reset() {
        fingers.clear()
        steering = null
        lastChord = null
    }

    private fun down(t: Touch): PlayInput? {
        val reported = find(t.id)?.let { release(it, false) } ?: expire(t.tMs) // a finger down again: its lift was missed
        val last = lastChord
        val chord = if (last != null && last.waiting > 0 && t.tMs - last.t0 <= CHORD_MS) last else Chord(t.tMs).also { lastChord = it }
        chord.waiting += 1
        fingers.add(Finger(t.id, t.x, t.y, t.tMs, chord))
        return reported
    }

    private fun move(t: Touch): PlayInput? {
        val f = find(t.id) ?: return null
        f.x = t.x
        f.y = t.y
        // The steering finger is no tap candidate any more, so only the others need the distance test.
        if (f === steering || !pastDead(t.x - f.x0, t.y - f.y0)) return expire(t.tMs)
        if (steering == null) steering = f
        return drop(f, false) ?: expire(t.tMs)
    }

    private fun up(t: Touch): PlayInput? {
        val f = find(t.id) ?: return null
        val tap = t.tMs - f.t0 <= TAP_MS && !pastDead(t.x - f.x0, t.y - f.y0)
        return release(f, tap) ?: expire(t.tMs)
    }

    private fun find(id: Int): Finger? {
        for (i in fingers.indices) if (fingers[i].id == id) return fingers[i]
        return null
    }

    private fun release(f: Finger, tapped: Boolean): PlayInput? {
        fingers.remove(f)
        if (steering === f) steering = null
        return drop(f, tapped)
    }

    // A finger is no longer a tap candidate: it moved, was held, was lifted late or was cancelled, or it tapped.
    private fun drop(f: Finger, tapped: Boolean): PlayInput? {
        val c = f.chord ?: return null
        f.chord = null
        c.waiting -= 1
        if (tapped) c.taps += 1
        if (c.waiting > 0 || c.taps == 0) return null
        return when (c.taps) {
            1 -> PlayInput.KICK
            2 -> PlayInput.WHERE
            else -> PlayInput.PAUSE
        }
    }

    // Drops the fingers held past the tap time, until one of them completes a tap; the rest wait for the next call.
    private fun expire(nowMs: Long): PlayInput? {
        for (i in fingers.indices) {
            val f = fingers[i]
            if (f.chord != null && nowMs - f.t0 > TAP_MS) drop(f, false)?.let { return it }
        }
        return null
    }
}
