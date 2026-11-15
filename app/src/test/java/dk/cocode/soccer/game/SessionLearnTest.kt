package dk.cocode.soccer.game

import dk.cocode.soccer.core.audio.Sound
import dk.cocode.soccer.core.input.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// "Learn the sounds" as the session runs it: the tour is driven by the frames, ends at Home, and any touch ends it.
class SessionLearnTest {
    @Test
    fun theTourRunsToItsEndAndGoesHomeWithTheAudioStopped() {
        val r = Rig()
        r.choose(r.en.learnSounds)
        assertEquals(Screen.Learn, r.screen)
        assertEquals(listOf("start"), r.audio.calls)
        r.frames(10.0)
        assertEquals(Screen.Learn, r.screen)
        r.frames(10.0) // the tour is about 15 seconds
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf("start", "stop"), r.audio.calls)
        assertEquals(r.game.learnEnd, r.spoken.last())
        assertTrue(Sound.CHIME in r.sink.plays && Sound.STEP in r.sink.plays)
        val plays = r.sink.plays.size
        r.frames(2.0)
        assertEquals(plays, r.sink.plays.size)
    }

    @Test
    fun anyTouchDuringTheTourReturnsHome() {
        val r = Rig()
        r.choose(r.en.learnSounds)
        r.frames(1.0)
        r.touch(Phase.DOWN, 1, 200f, 300f)
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf("start", "stop"), r.audio.calls)
        assertEquals(r.en.backToMenu, r.spoken.last())
        val plays = r.sink.plays.size
        r.touch(Phase.UP, 1, 200f, 300f)
        r.frames(3.0)
        assertEquals(plays, r.sink.plays.size)
        assertEquals(Screen.Home, r.screen)
    }

    @Test
    fun theScreenReaderComingOnEndsTheTour() {
        val r = Rig()
        r.choose(r.en.learnSounds)
        r.reader.on.value = true
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf("start", "stop"), r.audio.calls)
    }

    @Test
    fun backgroundDuringTheSoundTourGoesHome() {
        val r = Rig()
        r.choose(r.en.learnSounds)
        r.session.foreground(false)
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf("start", "stop"), r.audio.calls)
        r.session.foreground(true)
        assertEquals(Screen.Home, r.screen)
    }
}
