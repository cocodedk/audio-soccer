package dk.cocode.soccer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dk.cocode.soccer.game.Session

// A round: the pitch and the last thing said, under a layer that takes every touch (the whole screen is the control). What is
// drawn keeps clear of the system bars (insets); the layer of touches does not need to. App keeps the screen on.
@Composable
fun PlayScreen(session: Session, insets: WindowInsets = WindowInsets.safeDrawing) {
    val pitch = session.pitch.collectAsState()
    val said by session.lastLine.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(insets).padding(8.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth()) { PitchView { pitch.value } }
            Text(said)
        }
        Box(Modifier.fillMaxSize().touchSurface(session))
    }
}
