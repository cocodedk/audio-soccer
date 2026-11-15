package dk.cocode.soccer.ui

import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import dk.cocode.soccer.platform.installEngine
import org.junit.After
import org.junit.Assert.assertEquals
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
        scenario = ActivityScenario.launch(MainActivity::class.java)
        // The speech engine reports that it started, and the main thread takes it in.
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

    @Test
    fun backOnTheHomeScreenLeavesTheApp() {
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        shadowOf(Looper.getMainLooper()).idle()
        var finishing = false
        scenario.onActivity { finishing = it.isFinishing }
        assertTrue(finishing)
    }

    @Test
    fun theFramesDriveARoundAndLeavingPausesIt() {
        compose.onNodeWithText("Find the ball").performSemanticsAction(SemanticsActions.OnClick)
        repeat(80) { compose.mainClock.advanceTimeByFrame() }
        assertTrue(events().contains("screen Play(level=1)"))
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
}
