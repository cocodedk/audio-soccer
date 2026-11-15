package dk.cocode.soccer.game

import dk.cocode.soccer.core.input.MenuInput
import dk.cocode.soccer.core.input.Phase
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.platform.TtsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The speech engine: none, a late one, one without Persian, and the switching of engines. SessionEngineFallbackTest has
// the engines giving way to one another.
class SessionEngineTest {
    @Test
    fun noEngineBlocksHandOver() {
        val r = Rig(readerOn = true, state = TtsState.NO_ENGINE)
        assertEquals(Screen.NoEngine, r.screen)
        assertEquals(listOf(r.en.noEngine), r.session.body.value)
        assertTrue(r.items.isEmpty())
        r.session.choose(0)
        r.session.menu(MenuInput.Activate)
        r.reader.on.value = false
        r.reader.on.value = true
        assertEquals(Screen.NoEngine, r.screen)
        assertTrue(r.logged("screen").none { it.contains("HandOver") })
    }

    @Test
    fun noEngineStaysQuietAndTouchesDoNothing() {
        val r = Rig(state = TtsState.NO_ENGINE)
        r.touch(Phase.DOWN, 1, 50f, 50f)
        r.frames(1.0)
        assertTrue(r.spoken.isEmpty())
        assertTrue(r.session.back().not())
    }

    @Test
    fun anEngineThatIsStillLoadingIsWaitedForUpToFiveSeconds() {
        val r = Rig(state = TtsState.LOADING)
        assertTrue(r.spoken.isEmpty())
        r.choose(r.en.levelNames[0])
        assertEquals(Screen.Home, r.screen)
        r.frames(4.9)
        assertEquals(Screen.Home, r.screen)
        r.speech.state.value = TtsState.READY
        assertEquals(listOf(r.en.greeting), r.spoken)
        r.start(1)
        assertEquals(Screen.Play(1), r.screen)
    }

    // Every engine gets its five seconds: the default (which is eSpeak here), then the other installed ones.
    @Test
    fun enginesStillLoadingAfterFiveSecondsEachAreNoEngineUntilOneComesUp() {
        val r = Rig(state = TtsState.LOADING)
        r.frames(5.5)
        assertEquals(listOf(null, GOOGLE.pkg), r.speeches.map { it.pkg })
        assertEquals(Screen.Home, r.screen)
        r.frames(5.5)
        assertEquals(Screen.NoEngine, r.screen)
        assertEquals(listOf(r.en.noEngine), r.session.body.value)
        assertTrue(r.items.isEmpty())
        r.speech.state.value = TtsState.READY
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf(r.en.greeting), r.spoken)
        r.start(1)
        assertEquals(Screen.Play(1), r.screen)
    }

    @Test
    fun anEngineThatFailsAfterTheLimitStaysNoEngine() {
        val r = Rig(state = TtsState.LOADING)
        r.frames(5.5)
        r.speech.state.value = TtsState.NO_ENGINE
        assertEquals(Screen.NoEngine, r.screen)
        assertTrue(r.spoken.isEmpty())
    }

    @Test
    fun aSlowSavedEngineGivesWayToTheSystemOneAndThatOneIsKept() {
        val r = Rig(savedEngine = "slow.engine", states = mapOf("slow.engine" to TtsState.LOADING))
        assertEquals(Screen.Home, r.screen)
        r.frames(5.5)
        assertEquals(listOf("slow.engine", null), r.speeches.map { it.pkg })
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf(r.en.greeting), r.spoken)
        assertEquals(ESPEAK.pkg, r.prefs.enginePkgNow())
    }

    @Test
    fun anEngineSwitchThatTimesOutCarriesOnWhenTheSlowEngineComesUp() {
        val r = Rig()
        r.choose(r.en.settings)
        r.states = mapOf(GOOGLE.pkg to TtsState.LOADING, null to TtsState.LOADING)
        r.choose(r.en.voiceEngine(ESPEAK.label))
        r.frames(11.0)
        assertEquals(Screen.NoEngine, r.screen)
        r.speech.state.value = TtsState.READY
        assertEquals(Screen.Home, r.screen)
    }

    @Test
    fun anEngineSwitchIsAskedForOnceAtATime() {
        val r = Rig(state = TtsState.LOADING)
        r.speech.state.value = TtsState.READY
        r.choose(r.en.settings)
        val installed = r.speeches.size
        r.choose(r.en.voiceEngine(ESPEAK.label))
        assertEquals(installed + 1, r.speeches.size)
        // The new engine is still loading: another press waits for it.
        r.session.choose(1)
        assertEquals(installed + 1, r.speeches.size)
        r.speech.state.value = TtsState.READY
        assertEquals(r.en.voiceEngine(GOOGLE.label), r.speech.said.last())
    }

    @Test
    fun withOneEngineThereIsNothingToSwitchTo() {
        val r = Rig(engines = listOf(ESPEAK))
        r.choose(r.en.settings)
        r.choose(r.en.voiceEngine(ESPEAK.label))
        assertEquals(1, r.speeches.size)
    }

    @Test
    fun persianWithoutVoiceSpeaksEnglish() {
        val r = Rig(lang = Lang.FA, voices = setOf(Lang.EN))
        assertEquals(r.en.levelNames, r.items.take(3))
        assertEquals(listOf(r.en.englishOnly, r.en.greeting), r.spoken)
        assertEquals(1, r.spoken.count { it == r.en.englishOnly })
    }

    @Test
    fun persianWithAVoiceSpeaksPersian() {
        val r = Rig(lang = Lang.FA)
        assertEquals(listOf(r.fa.greeting), r.spoken)
        assertEquals(r.fa.levelNames, r.items.take(3))
    }

    // Found by playing it on an emulator; see Talk.nextLanguage.
    @Test
    fun theLanguageIsSetOnAnEngineThatHasBeenStopped() {
        val r = Rig()
        r.choose(r.en.settings)
        r.speech.calls.clear()
        r.choose(r.en.language)
        assertEquals(listOf("stop", "language"), r.speech.calls)
    }

    @Test
    fun anEnglishOnlyEngineIsSaidAgainWhenPersianIsChosenOnIt() {
        val r = Rig(voices = setOf(Lang.EN))
        r.choose(r.en.settings)
        r.choose(r.en.language)
        assertEquals(listOf(r.en.language, r.en.englishOnly), r.spoken.takeLast(2))
        assertEquals(r.en.language, r.items[0])
    }
}
