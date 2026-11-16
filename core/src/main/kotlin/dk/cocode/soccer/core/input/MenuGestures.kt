package dk.cocode.soccer.core.input

import kotlin.math.abs
import kotlin.math.hypot

sealed interface MenuInput {
    data class Focus(val index: Int) : MenuInput
    data object Next : MenuInput
    data object Prev : MenuInput
    data object Activate : MenuInput
    data object Repeat : MenuInput // say again the option the cursor is on
}

// The touch-to-hear menu, for a player with the screen reader off, close to what TalkBack does. The options are equal
// bands down the screen (count() of them over heightDp()).
//  - A finger still down after TAP_MS explores: Focus(band) at once, and again each time it crosses into another band.
//  - A stroke of SWIPE_DP or more lifted within TAP_MS is a swipe: right or down is Next, left or up is Prev.
//  - A stroke that a second finger joins within TAP_MS never explores: when the followed finger lifts after
//    travelling SWIPE_DP or more it is Next or Prev at any speed, a two-finger swipe as slow as a scroll. Lifted
//    within TAP_MS it is otherwise judged as one finger would be (a quick two-finger tap is still a tap); later,
//    nothing.
//  - A tap followed by a second tap landing within DOUBLE_MS of its lift is Activate, wherever it lands.
//  - A tap on its own is Repeat from tick, wherever it lands, once DOUBLE_MS pass with no second tap.
// Anything else (a stroke that moved DEAD_DP or more without being a swipe, or one lifted after TAP_MS without having
// explored) yields nothing. Only the first finger down is followed; a finger landing meanwhile marks it as a two-finger
// stroke, or makes it explore if it has been down past TAP_MS without a frame saying so. CANCEL drops the stroke and
// any waiting tap, whichever finger it names, and reset() does the same. A finger it has not seen is ignored, so a
// stroke already under way when the screen changed ends quietly.
//
// Call tick every frame, with the same clock as Touch.tMs: a held finger sends no event at TAP_MS. A tap that is
// waiting for its second one is also reported by the next finger that lands, if that is later than DOUBLE_MS.
class MenuGestures(private val count: () -> Int, private val heightDp: () -> Float) {
    // The stroke being followed. secondTap: it landed within DOUBLE_MS of a tap's lift.
    private class Stroke(val id: Int, val x0: Float, val y0: Float, val t0: Long, val secondTap: Boolean) {
        var y = y0
        var moved = false
        var exploring = false
        var twoFinger = false // a second finger joined it before it explored
        var band = 0 // while exploring, the band last focused
    }

    private var stroke: Stroke? = null
    private var tapUpMs: Long? = null // the lift of a tap, while a second tap may still come

    fun on(phase: Phase, t: Touch): MenuInput? = when (phase) {
        Phase.DOWN -> down(t)
        Phase.MOVE -> move(t)
        Phase.UP -> up(t)
        Phase.CANCEL -> {
            reset()
            null
        }
    }

    fun tick(nowMs: Long): MenuInput? {
        val s = stroke
        if (s != null) return hold(s, nowMs)
        val up = tapUpMs ?: return null
        if (open(up, nowMs)) return null
        tapUpMs = null
        return MenuInput.Repeat
    }

    fun reset() {
        stroke = null
        tapUpMs = null
    }

    private fun down(t: Touch): MenuInput? {
        val s = stroke
        if (s != null && t.id != s.id) {
            val focus = hold(s, t.tMs) // past TAP_MS already, though no frame has ticked yet: it explores
            if (!s.exploring) s.twoFinger = true
            return focus
        }
        val up = tapUpMs
        tapUpMs = null
        val second = up != null && open(up, t.tMs)
        stroke = Stroke(t.id, t.x, t.y, t.tMs, second)
        return if (up != null && !second) MenuInput.Repeat else null
    }

    private fun move(t: Touch): MenuInput? {
        val s = stroke
        if (s == null || t.id != s.id) return null
        s.y = t.y
        if (!s.exploring) {
            if (!s.moved) s.moved = pastDead(t.x - s.x0, t.y - s.y0)
            return hold(s, t.tMs)
        }
        val b = bandOf(t.y)
        if (b == s.band) return null
        s.band = b
        return MenuInput.Focus(b)
    }

    private fun up(t: Touch): MenuInput? {
        val s = stroke
        if (s == null || t.id != s.id) return null
        stroke = null
        if (s.exploring) return null
        val inTime = t.tMs - s.t0 <= TAP_MS
        val dx = t.x - s.x0
        val dy = t.y - s.y0
        val swiped = if (inTime || s.twoFinger) swipe(dx, dy) else null
        return swiped ?: when {
            !inTime || s.moved || pastDead(dx, dy) -> null
            s.secondTap -> MenuInput.Activate
            else -> {
                tapUpMs = t.tMs
                null
            }
        }
    }

    // A finger still down after TAP_MS explores: it focuses the band it is in at once.
    private fun hold(s: Stroke, nowMs: Long): MenuInput? {
        if (s.exploring || s.twoFinger || nowMs - s.t0 <= TAP_MS) return null
        s.exploring = true
        s.band = bandOf(s.y)
        return MenuInput.Focus(s.band)
    }

    // A stroke of SWIPE_DP or more, by its main axis: right or down is Next, left or up is Prev. Shorter is nothing.
    private fun swipe(dx: Float, dy: Float): MenuInput? {
        if (hypot(dx, dy) < SWIPE_DP) return null
        val along = if (abs(dx) >= abs(dy)) dx else dy
        return if (along > 0) MenuInput.Next else MenuInput.Prev
    }

    // A second tap landing at nowMs, after a tap lifted at upMs, makes a double-tap.
    private fun open(upMs: Long, nowMs: Long) = nowMs - upMs <= DOUBLE_MS

    private fun bandOf(y: Float): Int {
        val n = count()
        return (y * n / heightDp()).toInt().coerceIn(0, maxOf(n - 1, 0))
    }
}
