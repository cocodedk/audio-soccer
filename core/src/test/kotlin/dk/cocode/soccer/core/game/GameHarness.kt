package dk.cocode.soccer.core.game

import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.World
import dk.cocode.soccer.core.sim.arrows
import dk.cocode.soccer.core.sim.seeded
import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.gameStrings
import kotlin.math.hypot

val UP = arrows("Up") // toward the goal
val DOWN = arrows("Down")
val LEFT = arrows("Left")

private const val FRAME = 0.05

// A game with a seeded start, the lines it said, the rounds it won and the levels it finished.
// game.test.mjs runs game.js under node with no sound or speech engine: here the lines are just collected.
class GameHarness(seed: Int = 1, lang: Lang = Lang.EN, records: Records = Records()) {
    val strings = gameStrings(lang)
    val said = mutableListOf<Line>()
    val done = mutableListOf<LevelResult>()
    var wins = 0 // rounds won
    val game = Game(seeded(seed), strings, { said += it }, { done += it }, records) { wins += 1 }
    val world: World get() = checkNotNull(game.world)

    fun texts() = said.map { it.text }

    // Frames of 50 ms, up to secs, with a fixed direction held.
    fun tick(secs: Double, dir: Vec = Vec.ZERO) {
        var t = 0.0
        while (t < secs) {
            game.update(FRAME, dir)
            t += FRAME
        }
    }

    // The pause between rounds (3 s) and some slack.
    fun nextRound() = tick(3.2)

    // Kick, then let the frame with the kick pass.
    fun kick() {
        game.action(Action.KICK)
        game.update(FRAME, Vec.ZERO)
    }

    // Walk straight at the ball until the round ends (level 1).
    fun walkToBall() {
        var i = 0
        while (i < 4000 && game.phase == Phase.PLAY) {
            val dx = world.ball.x - world.player.x
            val dy = world.ball.y - world.player.y
            val d = hypot(dx, dy).takeIf { it != 0.0 } ?: 1.0
            game.update(1.0 / 60, Vec(dx / d, dy / d))
            i++
        }
    }

    fun place(player: Vec, ball: Vec, vel: Vec = Vec.ZERO) {
        world.player = player
        world.ball = ball
        world.ballVel = vel
    }
}
