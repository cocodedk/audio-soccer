package dk.cocode.soccer.core.sim

// Which side of the pitch: the player's left or right board, or the end wall (END only on a Bump).
enum class Side { LEFT, RIGHT, END }

// What happened in a simulation step, one subtype per event name world.js and ball.js emit.
sealed interface Event {
    data class Bump(val side: Side, val x: Double, val y: Double) : Event
    data class Tap(val x: Double, val y: Double) : Event
    data class Take(val x: Double, val y: Double) : Event
    data object NoKick : Event
    data class Kick(val x: Double, val y: Double) : Event
    data class Voy(val x: Double, val y: Double) : Event
    data class Board(val side: Side, val x: Double, val y: Double, val speed: Double) : Event
    data class Post(val x: Double, val y: Double, val speed: Double) : Event
    data class Save(val x: Double, val y: Double, val speed: Double) : Event
    data class Goal(val x: Double, val y: Double, val speed: Double) : Event

    // wide: which side of the goal a kicked ball went past (LEFT or RIGHT), null for any other end-wall hit.
    data class EndWall(val x: Double, val y: Double, val speed: Double, val wide: Side?) : Event
}
