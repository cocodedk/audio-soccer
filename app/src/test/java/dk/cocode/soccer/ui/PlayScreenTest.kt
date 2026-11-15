package dk.cocode.soccer.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import dk.cocode.soccer.game.Rig
import dk.cocode.soccer.game.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun play(r: Rig) {
        r.start(2)
        compose.setContent { SoccerTheme { PlayScreen(r.session) } }
    }

    @Test
    fun aTapOnTheScreenIsAKick() {
        val r = Rig()
        play(r)
        compose.onRoot().performTouchInput { click() }
        r.frames(0.1)
        assertTrue(r.log.contains("event Kick"))
    }

    @Test
    fun aFingerDraggedUpTheScreenRunsTowardTheGoal() {
        val r = Rig()
        play(r)
        val x = r.player.x
        compose.onRoot().performTouchInput {
            down(Offset(centerX, bottom - 100f))
            moveTo(Offset(centerX, bottom - 600f))
        }
        r.frames(1.0)
        assertTrue(r.player.x > x + 3)
        compose.onRoot().performTouchInput { up() }
        assertEquals(Screen.Play(2), r.screen)
    }
}
