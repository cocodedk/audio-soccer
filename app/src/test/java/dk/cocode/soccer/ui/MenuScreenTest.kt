package dk.cocode.soccer.ui

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import dk.cocode.soccer.game.Rig
import dk.cocode.soccer.game.Screen
import dk.cocode.soccer.platform.TtsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MenuScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val button = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun show(r: Rig) = compose.setContent { SoccerTheme { MenuScreen(r.session) } }

    // The screen with a screen reader on: the options are the buttons to choose from.
    private fun showWithReader() = Rig(readerOn = true).also(::show)

    private fun assertAllButtons(r: Rig) {
        for (text in r.items) compose.onNode(hasText(text).and(button)).assertExists()
    }

    @Test
    fun everyOptionIsAButtonWithItsText() {
        val r = showWithReader()
        assertEquals(6, r.items.size)
        assertAllButtons(r)
    }

    @Test
    fun clickingAButtonChoosesItAndTheScreenFollows() {
        val r = showWithReader()
        compose.onNodeWithText(r.en.settings).performClick()
        compose.waitForIdle()
        assertEquals(Screen.Settings, r.screen)
        assertAllButtons(r)
        assertTrue(r.spoken.isEmpty())
    }

    @Test
    fun theHandOverLineIsAPoliteLiveRegion() {
        val r = showWithReader()
        compose.onNodeWithText(r.en.levelNames[0]).performClick()
        compose.onNodeWithText(r.en.handOver)
            .assert(polite)
        compose.onNode(hasText(r.en.backToMenu).and(button)).assertExists()
    }

    @Test
    fun theHowToPlayLinesAreShown() {
        val r = showWithReader()
        compose.onNodeWithText(r.en.howToPlay).performClick()
        for (line in r.en.howToPlayLines) compose.onNodeWithText(line).assertExists()
    }

    @Test
    fun noEngineShowsItsMessageAndNoOption() {
        val r = Rig(readerOn = true, state = TtsState.NO_ENGINE)
        show(r)
        compose.onNodeWithText(r.en.noEngine).assertExists()
    }

    @Test
    fun theSoundTourShowsWhatItSaid() {
        val r = showWithReader()
        compose.onNodeWithText(r.en.learnSounds).performClick()
        compose.onNodeWithText(r.game.learnCalls)
            .assert(polite)
    }

    @Test
    fun withNoScreenReaderASwipeOnTheScreenMovesTheCursorAndSpeaksTheOption() {
        val r = Rig()
        show(r)
        compose.onRoot().performTouchInput { swipeDown() }
        compose.waitForIdle()
        assertEquals(1, r.session.focus.value)
        assertEquals(r.en.levelNames[1], r.spoken.last())
    }

    @Test
    fun theSelfVoicingLayerGoesWhenAScreenReaderComesOn() {
        val r = Rig()
        show(r)
        r.reader.on.value = true
        compose.waitForIdle()
        compose.onRoot().performTouchInput { swipeDown() }
        assertEquals(0, r.session.focus.value)
    }
}
