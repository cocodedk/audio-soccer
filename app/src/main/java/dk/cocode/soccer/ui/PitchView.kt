package dk.cocode.soccer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import dk.cocode.soccer.core.sim.PITCH_L
import dk.cocode.soccer.core.sim.PITCH_W
import dk.cocode.soccer.core.sim.POST_LEFT
import dk.cocode.soccer.core.sim.POST_RIGHT
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.game.Pitch
import kotlin.math.min

// Sizes as drawn, in metres of pitch: the ball and the posts larger than real, so they can be seen.
private const val BALL_DRAWN = 0.45
private const val PLAYER_DRAWN = 0.7
private const val POST_DRAWN = 0.3
private val LEFT_POST = Vec(PITCH_L, POST_LEFT)
private val RIGHT_POST = Vec(PITCH_L, POST_RIGHT)

// Where the pitch is on a canvas of width x height: as large as fits, in the middle, turned so that up the screen is toward the
// goal and right is the player's right. A point (x toward the goal, y from the player's left) lands at (y, PITCH_L - x).
internal class PitchFrame(width: Float, height: Float) {
    val scale = min(width / PITCH_W.toFloat(), height / PITCH_L.toFloat())
    val size = Size(scale * PITCH_W.toFloat(), scale * PITCH_L.toFloat())
    val left = (width - size.width) / 2
    val top = (height - size.height) / 2

    fun at(v: Vec) = Offset(left + scale * v.y.toFloat(), top + scale * (PITCH_L - v.x).toFloat())
}

// The pitch from above, as the shapes of Palette and no words. It takes no touches. The pitch is read as it is drawn, so a
// frame of play redraws this and recomposes nothing. Nothing but the field is drawn before a round has started.
@Composable
fun PitchView(pitch: () -> Pitch?) {
    Canvas(Modifier.fillMaxSize()) { drawPitch(pitch()) }
}

internal fun DrawScope.drawPitch(pitch: Pitch?) {
    val frame = PitchFrame(size.width, size.height)
    fun disc(colour: Color, v: Vec, radius: Double, style: DrawStyle = Fill) =
        drawCircle(colour, (radius * frame.scale).toFloat(), frame.at(v), style = style)

    drawRect(Palette.lines, Offset(frame.left, frame.top), frame.size, style = Stroke(frame.scale * 0.2f))
    drawLine(Palette.accent, frame.at(LEFT_POST), frame.at(RIGHT_POST), strokeWidth = frame.scale * 0.5f)
    disc(Palette.accent, LEFT_POST, POST_DRAWN)
    disc(Palette.accent, RIGHT_POST, POST_DRAWN)
    if (pitch == null) return
    pitch.keeper?.let { disc(Palette.keeper, it, PLAYER_DRAWN) }
    disc(Palette.ball, pitch.ball, BALL_DRAWN)
    disc(Palette.player, pitch.player, PLAYER_DRAWN)
    if (pitch.hasBall) disc(Palette.lines, pitch.player, PLAYER_DRAWN, Stroke(frame.scale * 0.15f))
}
