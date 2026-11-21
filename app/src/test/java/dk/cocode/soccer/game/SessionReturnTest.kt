package dk.cocode.soccer.game

import dk.cocode.soccer.core.input.MenuInput
import dk.cocode.soccer.platform.TtsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// A menu says where the player is when it comes up, and when the app comes back to it: its option under the cursor.
class SessionReturnTest {
    @Test
    fun enteringSettingsSpeaksTheOptionTheCursorIsOn() {
        val r = Rig()
        r.choose(r.en.settings)
        assertEquals(r.en.language, r.spoken.last())
    }

    @Test
    fun comingBackToHomeSpeaksTheOptionTheCursorIsOn() {
        val r = Rig()
        r.session.menu(MenuInput.Focus(2))
        val said = r.spoken.size
        r.leaveAndReturn()
        assertEquals(listOf(r.en.levelNames[2]), r.spoken.drop(said))
    }

    @Test
    fun comingBackToSettingsSpeaksTheOptionTheCursorIsOn() {
        val r = Rig()
        r.choose(r.en.settings)
        r.session.menu(MenuInput.Focus(1))
        val said = r.spoken.size
        r.leaveAndReturn()
        assertEquals(listOf(r.en.voiceEngine(ESPEAK.label)), r.spoken.drop(said))
    }

    @Test
    fun comingBackToHowToPlaySpeaksItsOption() {
        val r = Rig()
        r.choose(r.en.howToPlay)
        r.leaveAndReturn()
        assertEquals(r.en.backToMenu, r.spoken.last())
    }

    @Test
    fun comingBackToThePauseMenuSaysItIsPausedAndTheOptionTheCursorIsOn() {
        val r = Rig()
        r.start(1)
        r.leaveAndReturn()
        assertEquals(listOf(r.en.paused, r.en.resume), r.spoken.takeLast(2))
        r.session.menu(MenuInput.Next)
        r.leaveAndReturn()
        assertEquals(listOf(r.en.paused, r.en.giveUp), r.spoken.takeLast(2))
    }

    @Test
    fun withAScreenReaderOnNothingIsSpoken() {
        val r = Rig(readerOn = true)
        r.leaveAndReturn()
        assertTrue(r.spoken.isEmpty())
    }

    @Test
    fun beforeAnEngineSpeaksNothingIsSpoken() {
        val r = Rig(state = TtsState.LOADING)
        r.leaveAndReturn()
        assertTrue(r.spoken.isEmpty())
    }

    @Test
    fun theScreenReaderGoingOffOnSettingsSpeaksItsOption() {
        val r = Rig(readerOn = true)
        r.choose(r.en.settings)
        r.reader.on.value = false
        assertEquals(r.en.language, r.spoken.last())
    }
}
