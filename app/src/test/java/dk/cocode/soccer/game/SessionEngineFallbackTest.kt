package dk.cocode.soccer.game

import dk.cocode.soccer.platform.TtsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The engine that speaks giving way to another: a saved one, the system's, and the ones installed.
class SessionEngineFallbackTest {
    @Test
    fun aSavedEngineThatWillNotStartGivesWayToTheSystemOne() {
        val r = Rig(savedEngine = "gone.engine", states = mapOf("gone.engine" to TtsState.NO_ENGINE))
        assertEquals(listOf("gone.engine", null), r.speeches.map { it.pkg })
        assertEquals(1, r.speeches[0].shutdowns)
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf(r.en.greeting), r.spoken)
        // The engine that speaks is the one saved and named, not the one that failed.
        assertEquals(ESPEAK.pkg, r.prefs.enginePkgNow())
        r.choose(r.en.settings)
        assertEquals(r.en.voiceEngine(ESPEAK.label), r.items[1])
    }

    @Test
    fun anEngineSwitchThatFallsBackToTheSystemOneNamesAndSavesTheSystemOne() {
        val r = Rig()
        r.choose(r.en.settings)
        r.states = mapOf(GOOGLE.pkg to TtsState.NO_ENGINE)
        r.choose(r.en.voiceEngine(ESPEAK.label))
        assertEquals(listOf(null, GOOGLE.pkg, null), r.speeches.map { it.pkg })
        assertEquals(r.en.voiceEngine(ESPEAK.label), r.items[1])
        assertEquals(r.en.voiceEngine(ESPEAK.label), r.spoken.last())
        assertEquals(ESPEAK.pkg, r.prefs.enginePkgNow())
    }

    // A phone whose only engines were installed by the player has no default engine: Android does not make one the default.
    @Test
    fun withNoDefaultEngineTheFirstInstalledOneSpeaksAndIsKept() {
        val r = Rig(defaultEngine = null, states = mapOf(null to TtsState.NO_ENGINE))
        assertEquals(listOf(null, ESPEAK.pkg), r.speeches.map { it.pkg })
        assertEquals(1, r.speeches[0].shutdowns)
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf(r.en.greeting), r.spoken)
        assertEquals(ESPEAK.pkg, r.prefs.enginePkgNow())
        r.choose(r.en.settings)
        assertEquals(r.en.voiceEngine(ESPEAK.label), r.items[1])
    }

    // A default that is itself installed and would not start is not tried a second time.
    @Test
    fun aBrokenDefaultGivesWayToTheNextInstalledEngine() {
        val r = Rig(states = mapOf(null to TtsState.NO_ENGINE))
        assertEquals(listOf(null, GOOGLE.pkg), r.speeches.map { it.pkg })
        assertEquals(Screen.Home, r.screen)
        assertEquals(GOOGLE.pkg, r.prefs.enginePkgNow())
    }

    @Test
    fun aSavedEngineThatWillNotStartGivesWayToTheSystemOneAndThenToTheNextInstalledOne() {
        val r = Rig(savedEngine = ESPEAK.pkg, states = mapOf(ESPEAK.pkg to TtsState.NO_ENGINE, null to TtsState.NO_ENGINE))
        assertEquals(listOf(ESPEAK.pkg, null, GOOGLE.pkg), r.speeches.map { it.pkg })
        assertEquals(Screen.Home, r.screen)
        assertEquals(GOOGLE.pkg, r.prefs.enginePkgNow())
    }

    @Test
    fun noInstalledEngineIsNoEngine() {
        val r = Rig(engines = emptyList(), state = TtsState.NO_ENGINE)
        assertEquals(1, r.speeches.size)
        assertEquals(Screen.NoEngine, r.screen)
        assertTrue(r.spoken.isEmpty())
    }

    @Test
    fun noInstalledEngineThatStartsIsNoEngine() {
        val r = Rig(state = TtsState.NO_ENGINE)
        assertEquals(listOf(null, GOOGLE.pkg), r.speeches.map { it.pkg })
        assertEquals(Screen.NoEngine, r.screen)
    }

    @Test
    fun anEngineSwitchThatFailsEndsOnTheNoEngineScreen() {
        val r = Rig()
        r.choose(r.en.settings)
        r.states = mapOf(GOOGLE.pkg to TtsState.NO_ENGINE, null to TtsState.NO_ENGINE)
        r.choose(r.en.voiceEngine(ESPEAK.label))
        assertEquals(Screen.NoEngine, r.screen)
    }
}
