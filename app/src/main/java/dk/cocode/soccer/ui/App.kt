package dk.cocode.soccer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import dk.cocode.soccer.game.Screen
import dk.cocode.soccer.game.Session

private const val NANOS = 1_000_000_000.0

// The whole app: the screen the session is on, and the session's frames, one for each frame the display draws.
@Composable
fun App(session: Session) {
    val screen by session.screen.collectAsState()
    LaunchedEffect(session) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                session.frame(if (last == 0L) 0.0 else (now - last) / NANOS)
                last = now
            }
        }
    }
    SoccerTheme {
        // The surface gives the text its colour (white on black), which a bare background does not.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (screen is Screen.Play) PlayScreen(session) else MenuScreen(session)
        }
    }
}
