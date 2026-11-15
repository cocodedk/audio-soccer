package dk.cocode.soccer.game

import dk.cocode.soccer.core.input.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The cases the plan's Review Focus names for the phone's own behaviour: a screen reader coming on, the app going to the
// background, a finger held across a change of screen.
class SessionSafetyTest {
    @Test
    fun screenReaderOnPausesRound() {
        val r = Rig()
        r.start(2)
        r.frames(0.5)
        val said = r.spoken.size
        r.reader.on.value = true
        assertEquals(Screen.Paused(2), r.screen)
        assertEquals(listOf("start", "stop"), r.audio.calls)
        assertTrue(r.speech.stops > 0)
        r.frames(1.0)
        r.choose(r.en.giveUp)
        r.choose(r.en.backToMenu)
        assertEquals(said, r.spoken.size)
        assertEquals(Screen.Home, r.screen)
    }

    @Test
    fun resumingWithTheScreenReaderOnWaitsForItToGoOff() {
        val r = Rig()
        r.start(2)
        r.reader.on.value = true
        r.choose(r.en.resume)
        assertEquals(Screen.HandOver(2), r.screen)
        r.reader.on.value = false
        assertEquals(Screen.Play(2), r.screen)
        assertEquals(1, r.logged("round").size)
        assertEquals(listOf("start", "stop", "start"), r.audio.calls)
    }

    @Test
    fun givingUpWithTheScreenReaderOnHandsOverBeforeTheNextRound() {
        val r = Rig()
        r.start(2)
        r.reader.on.value = true
        r.choose(r.en.giveUp)
        assertEquals(Screen.HandOver(2), r.screen)
        r.reader.on.value = false
        assertEquals(Screen.Play(2), r.screen)
        r.frames(3.2)
        assertEquals(2, r.logged("round").size)
    }

    @Test
    fun backgroundPausesAndSilences() {
        val r = Rig()
        r.start(2)
        r.frames(0.5)
        val said = r.spoken.size
        val silences = r.sink.silences
        r.session.foreground(false)
        assertEquals(Screen.Paused(2), r.screen)
        assertTrue(r.speech.stops > 0)
        assertEquals(listOf("start", "stop"), r.audio.calls)
        assertTrue(r.sink.silences > silences)
        r.frames(1.0)
        assertEquals(said, r.spoken.size)
        r.session.foreground(true)
        assertEquals(listOf(r.en.paused, r.en.resume), r.spoken.takeLast(2))
        assertEquals(Screen.Paused(2), r.screen)
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

    @Test
    fun touchHeldAcrossScreensIsIgnored() {
        val r = Rig()
        r.touch(Phase.DOWN, 1, 50f, 150f)
        r.choose(r.en.settings)
        val said = r.spoken.size
        r.now += 100
        r.touch(Phase.UP, 1, 50f, 150f) // a tap, but of the old screen
        r.now += 500
        r.session.frame(0.016)
        assertEquals(said, r.spoken.size)
        // The next touch works as usual.
        r.touch(Phase.DOWN, 2, 50f, 300f)
        r.now += 400
        r.session.frame(0.016)
        assertEquals(r.en.voiceEngine(ESPEAK.label), r.spoken.last())
    }

    @Test
    fun aTapWaitingForItsSecondIsDroppedWhenTheScreenChanges() {
        val r = Rig()
        r.tap(1, 50f, 150f)
        r.choose(r.en.settings)
        r.tap(2, 50f, 150f) // would have been the double-tap that chooses the first option
        r.now += 500
        r.session.frame(0.016)
        assertEquals(Screen.Settings, r.screen)
        assertEquals(r.en.language, r.items[0]) // not stepped to Persian
        assertEquals(r.en.language, r.spoken.last()) // a tap on its own only says the option
    }

    @Test
    fun aFingerSteeringWhenTheRoundPausesDoesNotRunOnAfterResume() {
        val r = Rig()
        r.start(2)
        r.touch(Phase.DOWN, 1, 100f, 400f)
        r.touch(Phase.MOVE, 1, 100f, 300f)
        r.frames(0.5)
        val at = r.player
        r.pause()
        r.choose(r.en.resume)
        r.touch(Phase.MOVE, 1, 100f, 250f)
        r.frames(0.5)
        assertEquals(at, r.player)
    }
}
