package dk.cocode.soccer.game

import dk.cocode.soccer.core.audio.CHIME_FULL_BEAT
import dk.cocode.soccer.core.audio.CHIME_GAIN
import dk.cocode.soccer.core.audio.STEP_GAIN
import dk.cocode.soccer.core.audio.Sound
import dk.cocode.soccer.core.audio.SoundSink
import dk.cocode.soccer.core.audio.TICK_BEHIND_MAKEUP
import dk.cocode.soccer.core.audio.TICK_GAIN
import dk.cocode.soccer.core.cues.beaconEvery
import dk.cocode.soccer.core.cues.distanceGain
import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.text.GameStrings

private const val NAME_KEY = "learn"
private const val READER_WAIT_BASE = 1.0 // seconds a name is given to be heard in the screen reader's voice
private const val READER_WAIT_PER_CHAR = 0.1
private const val SPOT = 5.0 // m: the ball ahead of the listener, or behind
private const val BOARD_DISTANCE = 3.0 // m from the listener to the board knocked

// The sound tour of "Learn the sounds", from prototype/js/learn.js, speakers path (no panning): each step is named,
// the name is given time to be heard, then its sound plays. Driven by update(dt) once a frame. A sound is a time in
// seconds after the name and what to do then. isSaying asks the speech queue; readerOn says the player's own screen
// reader speaks the lines, so the queue is silent and the wait is a guess as long as the trial's.
internal class Learn(
    private val strings: GameStrings,
    private val say: (Line) -> Unit,
    private val isSaying: (String) -> Boolean,
    private val sink: SoundSink,
    private val readerOn: () -> Boolean,
) {
    private class Cue(val at: Double, val go: () -> Unit)

    private class Step(val name: String, val length: Double, val cues: List<Cue>)

    private val steps = listOf(
        Step(strings.learnCalls, 3.2, listOf(strings.callAhead, strings.callLeft, strings.callRight, strings.callBehind)
            .mapIndexed { i, word -> Cue(i * 0.8) { say(menuLine(word)) } }),
        Step(strings.learnTarget, 0.0, emptyList()),
        Step(strings.learnAhead, 2.0, ticks(behind = false)),
        Step(strings.learnBehind, 2.0, ticks(behind = true)),
        Step(strings.learnChimeNear, 4.5, chimes()),
        Step(strings.learnBoard, 0.9, listOf(Cue(0.0) { sink.play(Sound.BOARD, distanceGain(BOARD_DISTANCE)) })),
        Step(strings.learnFeet, 1.5, listOf(0.0, 0.35, 0.7, 1.05).map { Cue(it) { sink.play(Sound.STEP, STEP_GAIN) } }),
        Step(strings.learnVoy, 1.2, listOf(Cue(0.0) { say(menuLine(strings.voy).copy(pitch = 1.4f)) })),
    )

    private var index = 0
    private var naming = false
    private var clock = 0.0 // seconds into the name, then into the sounds
    private var fired = 0

    private fun ticks(behind: Boolean): List<Cue> {
        val gain = TICK_GAIN * (if (behind) TICK_BEHIND_MAKEUP else 1.0) * distanceGain(SPOT)
        return listOf(0.0, 0.6, 1.2).map { Cue(it) { sink.play(Sound.TICK, gain, behind) } }
    }

    // The goal chime from 30, 18 and 6 m (6 three times): two seconds apart, then one, then half a second.
    private fun chimes(): List<Cue> {
        var at = 0.0
        return listOf(30.0, 18.0, 6.0, 6.0, 6.0).map { d ->
            val every = beaconEvery(d)
            Cue(at) { sink.play(Sound.CHIME, CHIME_GAIN, stretch = every / CHIME_FULL_BEAT) }.also { at += every }
        }
    }

    fun start() = name()

    private fun name() {
        naming = true
        clock = 0.0
        fired = 0
        say(menuLine(steps[index].name).copy(key = NAME_KEY))
    }

    // Advances the tour by dt seconds. True once it has ended.
    fun update(dt: Double): Boolean {
        val step = steps.getOrNull(index) ?: return true
        clock += dt
        if (naming) {
            val heard = if (readerOn()) clock >= READER_WAIT_BASE + READER_WAIT_PER_CHAR * step.name.length else !isSaying(NAME_KEY)
            if (!heard) return false
            naming = false
            clock = 0.0
        }
        while (fired < step.cues.size && step.cues[fired].at <= clock) step.cues[fired++].go()
        if (clock < step.length) return false
        index += 1
        if (index < steps.size) {
            name()
            return false
        }
        say(menuLine(strings.learnEnd))
        return true
    }
}
