package dk.cocode.soccer.ui

import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import dk.cocode.soccer.platform.installEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowTextToSpeech
import java.util.Locale

// The activity on the Robolectric phone, with a speech engine installed so the game starts. The frame loop never lets Compose
// go idle, so the test clock is stepped by hand.
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun stepTheClockByHand() {
        compose.mainClock.autoAdvance = false
        installEngine(RuntimeEnvironment.getApplication(), "com.example.tts", "Example Speech")
        ShadowTextToSpeech.addLanguageAvailability(Locale.ENGLISH)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        // The speech engine reports that it is up, takes English and starts its first line, and the main thread takes it in.
        shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance()).onInitListener.onInit(TextToSpeech.SUCCESS)
        shadowOf(Looper.getMainLooper()).idle()
        compose.mainClock.advanceTimeByFrame()
    }

    @After
    fun close() {
        scenario.close()
    }

    private fun events() = ShadowLog.getLogsForTag("SoccerEvent").map { it.msg }

    private fun spoken() = ShadowLog.getLogsForTag("SoccerSpeech").map { it.msg }

    @Test
    fun launchesOnTheHomeScreenAndGreets() {
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Find the ball").assertExists()
        assertEquals(Lifecycle.State.RESUMED, scenario.state)
        assertTrue(spoken().any { it.startsWith("Audio Soccer.") })
    }

    private fun backFromHome(): Boolean {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        shadowOf(Looper.getMainLooper()).idle()
        var finishing = false
        scenario.onActivity { finishing = it.isFinishing }
        return finishing
    }

    // The game speaks for itself, and a drag in from the edge is the back gesture: the app must not close without a word.
    @Test
    fun backOnTheHomeScreenDoesNothingWhileTheGameSpeaksForItself() {
        assertFalse(backFromHome())
    }

    @Test
    fun backOnTheHomeScreenLeavesTheAppWhenAScreenReaderIsOn() {
        val manager = RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setTouchExplorationEnabled(true)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(backFromHome())
    }

    @Test
    fun theFramesDriveARoundAndLeavingPausesIt() {
        compose.onNodeWithText("Find the ball").performSemanticsAction(SemanticsActions.OnClick)
        // The drag intro is said first, and the round begins once the engine has finished its lines.
        playFrames()
        assertTrue(events().contains("screen Play(level=1)"))
        val intro = spoken().indexOfFirst { it.startsWith("Put a finger anywhere") }
        assertTrue(intro >= 0 && intro < spoken().indexOfFirst { it.startsWith("Round 1.") })
        assertTrue(events().any { it.startsWith("player x=") })
        scenario.moveToState(Lifecycle.State.STARTED)
        assertTrue(events().contains("screen Paused(level=1)"))
        scenario.moveToState(Lifecycle.State.RESUMED)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Resume").assertExists()
        assertTrue(spoken().contains("Paused"))
        compose.onNodeWithText("Back to menu").performSemanticsAction(SemanticsActions.OnClick)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Find the ball").assertExists()
    }

    // The screen going dark would take the app to the background: it stays lit during a round and the sound tour, not on a menu.
    @Test
    fun theScreenStaysOnDuringARoundAndTheSoundTourOnly() {
        assertFalse(screenKeptOn())
        compose.onNodeWithText("Learn the sounds").performSemanticsAction(SemanticsActions.OnClick)
        settle()
        assertTrue(screenKeptOn())
        compose.onNodeWithText("Back to menu").performSemanticsAction(SemanticsActions.OnClick)
        settle()
        assertFalse(screenKeptOn())
        compose.onNodeWithText("Find the ball").performSemanticsAction(SemanticsActions.OnClick)
        playFrames()
        settle()
        assertTrue(screenKeptOn())
    }

    // Frames, with the speech engine letting each line it was given end.
    private fun playFrames() = repeat(80) {
        compose.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idle()
    }

    // The session sets whether the screen stays on in one frame, and the view follows in the next.
    private fun settle() = repeat(3) { compose.mainClock.advanceTimeByFrame() }

    // Compose sets it on the view of its own composition, somewhere below the activity's content.
    private fun screenKeptOn(): Boolean {
        fun View.keepsOn(): Boolean = keepScreenOn || (this is ViewGroup && children.any { it.keepsOn() })
        var on = false
        scenario.onActivity { on = it.findViewById<ViewGroup>(android.R.id.content).keepsOn() }
        return on
    }
}
