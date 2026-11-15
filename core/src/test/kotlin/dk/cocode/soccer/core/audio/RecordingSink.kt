package dk.cocode.soccer.core.audio

import dk.cocode.soccer.core.sim.Event
import dk.cocode.soccer.core.sim.World
import dk.cocode.soccer.core.sim.close
import org.junit.jupiter.api.Assertions.assertTrue

// A sink that writes down what it is asked to play and when (the test's own clock, advanced by run()), and the crowd levels.
class RecordingSink : SoundSink {
    class Played(val t: Double, val sound: Sound, val gain: Double, val behind: Boolean, val stretch: Double)

    var t = 0.0
    val plays = mutableListOf<Played>()
    val crowds = mutableListOf<Double>()

    override fun play(sound: Sound, gain: Double, behind: Boolean, stretch: Double) {
        plays += Played(t, sound, gain, behind, stretch)
    }

    override fun crowd(level: Double) {
        crowds += level
    }

    fun of(sound: Sound) = plays.filter { it.sound == sound }

    fun times(sound: Sound) = of(sound).map { it.t }

    // Updates the soundscape every dt for secs seconds; the events come with the first frame only.
    fun run(scape: Soundscape, w: World, secs: Double, dt: Double = 0.01, events: List<Event> = emptyList()) {
        var first = true
        val end = t + secs
        while (t < end - dt / 2) {
            t += dt
            scape.update(w, dt, if (first) events else emptyList())
            first = false
        }
    }
}

// The sounds came gap seconds apart (give or take a frame), and at least three of them.
fun assertEvery(gap: Double, times: List<Double>) {
    assertTrue(times.size >= 3, "only ${times.size} sounds")
    for (i in 1 until times.size) close(gap, times[i] - times[i - 1], 0.011)
}
