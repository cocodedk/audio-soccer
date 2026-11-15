package dk.cocode.soccer.game

import dk.cocode.soccer.core.input.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// When the screen is to stay on: a screen that goes dark takes the app to the background, which pauses or ends what it was doing.
class SessionAwakeTest {
    private val Rig.awake get() = session.awake.value

    @Test
    fun theScreenMayGoDarkOnTheMenus() {
        val r = Rig()
        r.frames(0.1)
        assertFalse(r.awake)
        r.choose(r.en.settings)
        r.frames(0.1)
        assertFalse(r.awake)
    }

    @Test
    fun theScreenStaysOnWhileARoundIsOnAndNotWhileItIsPaused() {
        val r = Rig()
        r.start(1)
        r.frames(0.1)
        assertTrue(r.awake)
        r.pause()
        r.frames(0.1)
        assertFalse(r.awake)
    }

    @Test
    fun theScreenStaysOnDuringTheSoundTour() {
        val r = Rig()
        r.choose(r.en.learnSounds)
        r.frames(0.1)
        assertEquals(Screen.Learn, r.screen)
        assertTrue(r.awake)
        r.touch(Phase.DOWN, 1, 50f, 50f)
        r.frames(0.1)
        assertFalse(r.awake)
    }

    @Test
    fun theScreenStaysOnWhileTheHowToPlayLinesAreBeingRead() {
        val r = Rig().holding()
        r.choose(r.en.howToPlay)
        r.frames(0.1)
        assertTrue(r.awake)
        repeat(r.en.howToPlayLines.size) {
            r.speech.release()
            r.frames(0.1)
        }
        assertFalse(r.awake) // read to the end: now it may go dark
    }

    @Test
    fun theHowToPlayLinesCutOffByLeavingNoLongerKeepTheScreenOn() {
        val r = Rig().holding()
        r.choose(r.en.howToPlay)
        r.frames(0.1)
        r.session.foreground(false)
        r.frames(0.1)
        assertFalse(r.awake)
    }
}
