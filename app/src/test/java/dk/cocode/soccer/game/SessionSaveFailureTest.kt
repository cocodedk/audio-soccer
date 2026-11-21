package dk.cocode.soccer.game

import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.platform.TtsState
import org.junit.Assert.assertEquals
import org.junit.Test

// A choice that cannot be written to disk is just not kept: the game carries on, and says so once in the log.
class SessionSaveFailureTest {
    @Test
    fun aLanguageThatCannotBeSavedStillTakesEffectAndTheFailureIsLoggedOnce() {
        val r = Rig()
        r.prefs.failWrites = true
        r.choose(r.en.settings)
        r.choose(r.en.language)
        assertEquals(r.fa.language, r.items[0])
        assertEquals(r.fa.language, r.spoken.last())
        r.choose(r.fa.language)
        assertEquals(r.en.language, r.items[0])
        assertEquals(Lang.EN, r.prefs.langNow()) // neither was kept
        assertEquals(1, r.logged("settings not saved").size)
    }

    @Test
    fun anEngineThatCannotBeSavedStillSpeaksAndTheGameStarts() {
        val r = Rig(savedEngine = "gone.engine", states = mapOf("gone.engine" to TtsState.NO_ENGINE), failWrites = true)
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf(r.en.greeting), r.spoken)
        assertEquals(1, r.logged("settings not saved").size)
        r.start(1)
        assertEquals(Screen.Play(1), r.screen)
    }
}
