package dk.cocode.soccer.game

import dk.cocode.soccer.platform.TtsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// A phone with no speech engine that speaks: reopening the app tries the engines again, as the player may have installed one.
class SessionEngineRetryTest {
    @Test
    fun comingBackWithAnEngineInstalledMovesOnToHome() {
        val r = Rig(state = TtsState.NO_ENGINE)
        assertEquals(Screen.NoEngine, r.screen)
        r.state = TtsState.READY
        r.leaveAndReturn()
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf(r.en.greeting), r.spoken)
        assertEquals(r.en.levelNames.size + 3, r.items.size)
        r.start(1)
        assertEquals(Screen.Play(1), r.screen)
    }

    @Test
    fun comingBackWithStillNoEngineStaysOnTheNoEngineScreen() {
        val r = Rig(state = TtsState.NO_ENGINE)
        val engines = r.speeches.size
        r.leaveAndReturn()
        assertEquals(Screen.NoEngine, r.screen)
        assertTrue(r.speeches.size > engines) // the engines were tried again
        assertTrue(r.spoken.isEmpty())
    }

    @Test
    fun anEngineSlowToStartIsNotStartedAgainWhileItIsBeingWaitedFor() {
        val r = Rig(state = TtsState.NO_ENGINE)
        r.state = TtsState.LOADING
        r.leaveAndReturn()
        val engines = r.speeches.size
        r.leaveAndReturn() // the start under way has not ended
        assertEquals(engines, r.speeches.size)
        r.speech.state.value = TtsState.READY
        assertEquals(Screen.Home, r.screen)
    }

    // The first start went on waiting for the last engine it tried, and must not greet the player a second time.
    @Test
    fun theEngineAnEarlierStartWasWaitingForDoesNotGreetAgainWhenItComesUp() {
        val r = Rig(state = TtsState.LOADING)
        r.frames(11.0)
        assertEquals(Screen.NoEngine, r.screen)
        val waitedFor = r.speech
        r.state = TtsState.READY
        r.leaveAndReturn()
        assertEquals(Screen.Home, r.screen)
        waitedFor.state.value = TtsState.READY
        assertEquals(listOf(r.en.greeting), r.spoken)
    }

    @Test
    fun comingBackToAMenuWithAnEngineDoesNotStartTheEnginesAgain() {
        val r = Rig()
        val engines = r.speeches.size
        r.leaveAndReturn()
        assertEquals(engines, r.speeches.size)
    }
}
