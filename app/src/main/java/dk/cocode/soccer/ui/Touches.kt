package dk.cocode.soccer.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import dk.cocode.soccer.core.input.Phase
import dk.cocode.soccer.core.input.Touch
import dk.cocode.soccer.game.Session

// Hands every finger on this surface to the session, in dp and with the time of the event (the clock of SystemClock.uptimeMillis,
// the one the session's frames run on), and takes the touches so nothing under it gets them. It is the self-voicing layer, which
// the screens put on only while no screen reader is on. A finger that is cancelled arrives as a lift.
fun Modifier.touchSurface(session: Session): Modifier = pointerInput(session) {
    awaitPointerEventScope {
        while (true) {
            for (change in awaitPointerEvent().changes) {
                val phase = when {
                    change.changedToDown() -> Phase.DOWN
                    change.changedToUp() -> Phase.UP
                    change.positionChanged() -> Phase.MOVE
                    else -> continue
                }
                session.resize(size.height / density)
                session.touch(phase, Touch(change.id.value.toInt(), change.position.x / density, change.position.y / density, change.uptimeMillis))
                change.consume()
            }
        }
    }
}
