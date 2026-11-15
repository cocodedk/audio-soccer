package dk.cocode.soccer.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.game.Pitch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PitchViewTest {
    @Test
    fun upTheScreenIsTowardTheGoalAndRightIsThePlayersRight() {
        val frame = PitchFrame(200f, 400f)
        assertEquals(0f, frame.at(Vec(40.0, 0.0)).y, 0.01f) // the goal line is the top edge
        assertEquals(400f, frame.at(Vec(0.0, 0.0)).y, 0.01f)
        assertEquals(0f, frame.at(Vec(20.0, 0.0)).x, 0.01f) // the player's left is the left edge
        assertEquals(200f, frame.at(Vec(20.0, 20.0)).x, 0.01f)
        assertEquals(100f, frame.at(Vec(40.0, 10.0)).x, 0.01f) // the middle of the goal
    }

    @Test
    fun aWideCanvasLeavesRoomAtTheSides() {
        val frame = PitchFrame(400f, 400f)
        assertEquals(10f, frame.scale, 0.01f)
        assertEquals(100f, frame.left, 0.01f)
        assertEquals(0f, frame.top, 0.01f)
        assertEquals(100f, frame.at(Vec(0.0, 0.0)).x, 0.01f)
    }

    private fun draw(pitch: Pitch?) {
        val bitmap = ImageBitmap(200, 400)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), Size(200f, 400f)) { drawPitch(pitch) }
    }

    @Test
    fun drawsTheEmptyPitchBeforeARound() = draw(null)

    @Test
    fun drawsThePlayerTheBallAndTheKeeper() {
        draw(Pitch(Vec(10.0, 5.0), Vec(12.0, 6.0), Vec(39.6, 10.0), hasBall = true))
        draw(Pitch(Vec(10.0, 5.0), Vec(30.0, 6.0), null, hasBall = false))
    }
}
