package dk.cocode.soccer.core.input

import kotlin.math.hypot

// What the Android shell hands the recognizers: a finger's event, already converted to dp (y grows down the screen)
// and stamped with the event's own time in ms. Time is only ever read from there and from tick(nowMs), which must be
// on the same clock.
enum class Phase { DOWN, MOVE, UP, CANCEL }

data class Touch(val id: Int, val x: Float, val y: Float, val tMs: Long)

// Thresholds, in dp and ms, as in prototype/phone/js/gestures.js.
internal const val DEAD_DP = 24f // a finger within this of where it landed has not moved
internal const val TAP_MS = 300L // a finger lifted within this, without moving, is a tap
internal const val CHORD_MS = 150L // fingers landing this close together belong to one tap
internal const val SWIPE_DP = 60f // a stroke at least this long is a swipe
internal const val DOUBLE_MS = 350L // a second tap landing within this of the first one's lift is a double-tap

// True once an offset from where a finger landed has left the dead zone.
internal fun pastDead(dx: Float, dy: Float) = hypot(dx, dy) >= DEAD_DP
