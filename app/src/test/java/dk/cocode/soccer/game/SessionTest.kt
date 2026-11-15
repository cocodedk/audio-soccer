package dk.cocode.soccer.game

import dk.cocode.soccer.core.input.MenuInput
import dk.cocode.soccer.core.input.Phase
import dk.cocode.soccer.core.input.PlayInput
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.text.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    @Test
    fun greetsOnHomeWhenScreenReaderOff() {
        val r = Rig()
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf(r.en.greeting), r.spoken)
        assertEquals(r.en.levelNames + listOf(r.en.learnSounds, r.en.howToPlay, r.en.settings), r.items)
    }

    @Test
    fun silentWhenScreenReaderOn() {
        val r = Rig(readerOn = true)
        r.session.menu(MenuInput.Focus(1))
        r.session.menu(MenuInput.Next)
        r.choose(r.en.settings)
        r.frames(1.0)
        assertTrue(r.spoken.isEmpty())
        assertEquals(Screen.Settings, r.screen)
        assertTrue(r.speech.said.isEmpty())
    }

    @Test
    fun focusSpeaksTheOption() {
        val r = Rig()
        r.session.menu(MenuInput.Focus(1))
        assertEquals(r.en.levelNames[1], r.spoken.last())
        assertEquals(1, r.session.focus.value)
        r.session.menu(MenuInput.Next)
        assertEquals(r.en.levelNames[2], r.spoken.last())
        r.session.menu(MenuInput.Prev)
        r.session.menu(MenuInput.Prev)
        r.session.menu(MenuInput.Prev)
        assertEquals(0, r.session.focus.value)
        assertEquals(r.en.levelNames[0], r.spoken.last())
        assertEquals(r.en.levelNames[0], r.session.lastLine.value)
    }

    @Test
    fun aFingerHeldOnAnOptionSpeaksItAndADoubleTapChoosesIt() {
        val r = Rig()
        r.touch(Phase.DOWN, 1, 50f, 150f)
        r.now += 400
        r.session.frame(0.016)
        assertEquals(r.en.levelNames[1], r.spoken.last())
        r.touch(Phase.UP, 1, 50f, 150f)
        r.now += 100
        r.doubleTap(50f, 400f)
        assertEquals(Screen.Play(2), r.screen)
    }

    @Test
    fun activateStartsTheLevel() {
        val r = Rig()
        r.session.menu(MenuInput.Focus(1))
        r.session.menu(MenuInput.Activate)
        assertEquals(Screen.Play(2), r.screen)
        assertTrue(r.spoken.contains(r.game.roundIntro(1, r.game.levelTask(2))))
        assertEquals(listOf("start"), r.audio.calls)
        assertTrue(r.logged("screen").contains("screen Play(level=2)"))
        assertTrue(r.logged("round").contains("round 1 level=2"))
    }

    @Test
    fun handOverStartsWhenScreenReaderGoesOff() {
        val r = Rig(readerOn = true)
        r.start(2)
        assertEquals(Screen.HandOver(2), r.screen)
        assertEquals(listOf(r.en.handOver), r.session.body.value)
        assertTrue(r.spoken.isEmpty())
        assertTrue(r.audio.calls.isEmpty())
        r.reader.on.value = false
        assertEquals(Screen.Play(2), r.screen)
        assertTrue(r.spoken.contains(r.game.roundIntro(1, r.game.levelTask(2))))
        assertEquals(listOf("start"), r.audio.calls)
    }

    @Test
    fun handOverCanBeLeftByItsButton() {
        val r = Rig(readerOn = true)
        r.start(1)
        r.choose(r.en.backToMenu)
        assertEquals(Screen.Home, r.screen)
        r.reader.on.value = false
        assertEquals(Screen.Home, r.screen)
        assertEquals(listOf(r.en.greeting), r.spoken)
    }

    @Test
    fun threeFingerTapPauses() {
        val r = Rig()
        r.start(2)
        for (id in 1..3) r.touch(Phase.DOWN, id, 50f + id, 300f)
        r.now += 50
        for (id in 1..3) r.touch(Phase.UP, id, 50f + id, 300f)
        assertEquals(Screen.Paused(2), r.screen)
        assertEquals(listOf(r.en.resume, r.en.giveUp, r.en.backToMenu), r.items)
        assertEquals(listOf(r.en.paused, r.en.resume), r.spoken.takeLast(2))
        assertEquals(listOf("start", "stop"), r.audio.calls)
    }

    @Test
    fun engineItemCyclesInstalledEngines() {
        val r = Rig()
        r.choose(r.en.settings)
        assertEquals(listOf(r.en.language, r.en.voiceEngine(ESPEAK.label), r.en.backToMenu), r.items)
        val first = r.speech
        r.choose(r.en.voiceEngine(ESPEAK.label))
        assertEquals(GOOGLE.pkg, r.speech.pkg)
        assertEquals(1, first.shutdowns)
        assertEquals(Lang.EN, r.speech.language)
        assertEquals(listOf(r.en.voiceEngine(GOOGLE.label)), r.speech.said)
        assertEquals(r.en.voiceEngine(GOOGLE.label), r.items[1])
        assertEquals(GOOGLE.pkg, r.prefs.enginePkgNow())
        r.choose(r.en.voiceEngine(GOOGLE.label))
        assertEquals(ESPEAK.pkg, r.speech.pkg)
        assertEquals(r.en.voiceEngine(ESPEAK.label), r.speech.said.last())
    }

    @Test
    fun theLanguageItemStepsToTheNextLanguageAndSpeaksItThere() {
        val r = Rig()
        r.choose(r.en.settings)
        r.choose(r.en.language)
        assertEquals(r.fa.language, r.items[0])
        assertEquals(r.fa.language, r.spoken.last())
        assertEquals(Lang.FA, r.speech.language)
        assertEquals(Lang.FA, r.prefs.langNow())
        r.choose(r.fa.language)
        assertEquals(r.en.language, r.spoken.last())
    }

    @Test
    fun backFromSettingsGoesHome() {
        val r = Rig()
        r.choose(r.en.settings)
        r.choose(r.en.backToMenu)
        assertEquals(Screen.Home, r.screen)
        assertEquals(r.en.backToMenu, r.spoken.last())
    }

    @Test
    fun howToPlayIsSpokenLineByLine() {
        val r = Rig()
        r.choose(r.en.howToPlay)
        assertEquals(Screen.Help, r.screen)
        assertEquals(r.en.howToPlayLines, r.spoken.takeLast(r.en.howToPlayLines.size))
        assertEquals(r.en.howToPlayLines, r.session.body.value)
        assertEquals(listOf(r.en.backToMenu), r.items)
    }

    @Test
    fun backGoesBackOneScreenAndTheSystemLeavesFromHome() {
        val r = Rig()
        assertFalse(r.session.back())
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

    @Test
    fun playInputsAreIgnoredOutsideARound() {
        val r = Rig()
        r.session.play(PlayInput.KICK)
        r.session.steer(Vec.ZERO)
        r.session.resize(800f)
        assertEquals(Screen.Home, r.screen)
    }
}
