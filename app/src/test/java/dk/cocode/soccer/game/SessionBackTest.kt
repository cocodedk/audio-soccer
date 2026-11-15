package dk.cocode.soccer.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The system's back, screen by screen.
class SessionBackTest {
    // A self-voicing player drags in from the screen's edge, which is the back gesture: it must not close the app unheard.
    @Test
    fun backOnHomeDoesNothingWhileTheGameSpeaksForItselfAndLeavesWithAScreenReader() {
        val r = Rig()
        assertTrue(r.session.back())
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf(r.en.greeting), r.spoken)
        r.reader.on.value = true
        assertFalse(r.session.back())
    }

    @Test
    fun backGoesBackOneScreen() {
        val r = Rig()
        r.choose(r.en.settings)
        assertTrue(r.session.back())
        assertEquals(Screen.Home, r.screen)
        r.start(1)
        assertTrue(r.session.back())
        assertEquals(Screen.Paused(1), r.screen)
        assertTrue(r.session.back()) // back on the pause menu goes on with the round
        assertEquals(Screen.Play(1), r.screen)
        assertTrue(r.session.back())
        r.choose(r.en.backToMenu)
        assertEquals(Screen.Home, r.screen)
        r.choose(r.en.howToPlay)
        assertTrue(r.session.back())
        assertEquals(Screen.Home, r.screen)
    }
}
