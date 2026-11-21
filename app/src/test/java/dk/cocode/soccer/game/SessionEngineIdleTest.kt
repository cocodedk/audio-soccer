package dk.cocode.soccer.game

import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.platform.TtsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The engine is stopped before its language is set, whichever way it comes up: see Talk.speakLanguage.
class SessionEngineIdleTest {
    @Test
    fun theEngineIsStoppedBeforeItsLanguageIsSetWhenItComesUp() {
        val persian = Rig(lang = Lang.FA)
        assertEquals(listOf("stop", "language"), persian.speech.calls)
        assertEquals(Lang.FA, persian.speech.language)
    }

    @Test
    fun aSlowEngineIsStoppedBeforeItsLanguageIsSetToo() {
        val r = Rig(state = TtsState.LOADING)
        r.frames(11.0)
        assertEquals(Screen.NoEngine, r.screen)
        assertTrue(r.speech.calls.isEmpty())
        r.speech.state.value = TtsState.READY
        assertEquals(listOf("stop", "language"), r.speech.calls)
    }

    @Test
    fun anEngineSwitchStopsTheNewEngineBeforeItsLanguageIsSet() {
        val r = Rig(lang = Lang.FA)
        r.choose(r.fa.settings)
        r.choose(r.fa.voiceEngine(ESPEAK.label))
        assertEquals(GOOGLE.pkg, r.speech.pkg)
        assertEquals(listOf("stop", "language"), r.speech.calls)
    }
}
