package dk.cocode.soccer.ui

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.KEEPER_X
import dk.cocode.soccer.core.sim.PITCH_L
import dk.cocode.soccer.core.sim.POST_LEFT
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.game.Pitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val WIDTH = 200
private const val HEIGHT = 400

// The drawing is read back from the pixels of a 200 x 400 bitmap (native graphics, see robolectric.properties): 10 px to the
// metre, the goal line along the top edge.
@RunWith(RobolectricTestRunner::class)
class PitchViewTest {
    private val frame = PitchFrame(WIDTH.toFloat(), HEIGHT.toFloat())
    private val empty = 0 // a pixel nothing was drawn on
    private val keeper = Vec(KEEPER_X, GOAL.y)
    private val player = Vec(10.0, 5.0)
    private val ball = Vec(12.0, 6.0)

    @Test
    fun upTheScreenIsTowardTheGoalAndRightIsThePlayersRight() {
        assertEquals(0f, frame.at(Vec(PITCH_L, 0.0)).y, 0.01f) // the goal line is the top edge
        assertEquals(HEIGHT.toFloat(), frame.at(Vec(0.0, 0.0)).y, 0.01f)
        assertEquals(0f, frame.at(Vec(20.0, 0.0)).x, 0.01f) // the player's left is the left edge
        assertEquals(WIDTH.toFloat(), frame.at(Vec(20.0, 20.0)).x, 0.01f)
        assertEquals(100f, frame.at(GOAL).x, 0.01f) // the middle of the goal
    }

    @Test
    fun aWideCanvasLeavesRoomAtTheSides() {
        val wide = PitchFrame(400f, 400f)
        assertEquals(10f, wide.scale, 0.01f)
        assertEquals(100f, wide.left, 0.01f)
        assertEquals(0f, wide.top, 0.01f)
        assertEquals(100f, wide.at(Vec(0.0, 0.0)).x, 0.01f)
    }

    private fun draw(pitch: Pitch?): Bitmap {
        val image = ImageBitmap(WIDTH, HEIGHT)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(WIDTH.toFloat(), HEIGHT.toFloat())) { drawPitch(pitch) }
        return image.asAndroidBitmap()
    }

    private fun pitch(keeper: Vec? = null, hasBall: Boolean = false) = Pitch(player, ball, keeper, hasBall)

    private fun Bitmap.at(v: Vec) = frame.at(v).let { getPixel(it.x.toInt(), it.y.toInt()) }

    @Test
    fun theEmptyPitchHasItsFieldLineAndAGoalLineInTheAccentColour() {
        val bitmap = draw(null)
        assertEquals(Palette.lines.toArgb(), bitmap.getPixel(0, HEIGHT / 2)) // the left board
        assertEquals(Palette.lines.toArgb(), bitmap.getPixel(WIDTH / 2, HEIGHT - 1)) // the end behind the player
        assertEquals(Palette.accent.toArgb(), bitmap.getPixel(WIDTH / 2, 1)) // the goal line, between the posts
        assertEquals(Palette.accent.toArgb(), bitmap.at(Vec(PITCH_L, POST_LEFT))) // a post
        assertEquals(empty, bitmap.at(Vec(20.0, 10.0))) // the field itself is not painted
    }

    @Test
    fun thePlayerTheBallAndTheKeeperAreDrawnWhereTheyAreInTheirOwnColours() {
        val bitmap = draw(pitch(keeper))
        assertEquals(Palette.player.toArgb(), bitmap.at(player))
        assertEquals(Palette.ball.toArgb(), bitmap.at(ball))
        assertEquals(Palette.keeper.toArgb(), bitmap.at(keeper))
        assertEquals(empty, bitmap.at(Vec(30.0, 15.0))) // nowhere else
    }

    @Test
    fun noKeeperMeansNoKeeperDrawn() {
        assertEquals(empty, draw(pitch()).at(Vec(KEEPER_X, 6.0))) // clear of the goal line and the posts
        assertNotEquals(Palette.keeper.toArgb(), draw(pitch()).at(keeper))
    }

    @Test
    fun aPlayerWithTheBallHasARingRoundThem() {
        val ring = Vec(player.x, player.y + 0.75) // just outside the disc, on the ring's band
        assertEquals(empty, draw(pitch()).at(ring))
        assertNotEquals(empty, draw(pitch(hasBall = true)).at(ring))
    }
}
