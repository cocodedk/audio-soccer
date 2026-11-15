package dk.cocode.soccer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dk.cocode.soccer.game.Screen
import dk.cocode.soccer.game.Session

private val Polite = Modifier.semantics { liveRegion = LiveRegionMode.Polite }

// Every screen but the pitch: its text, then its options as buttons filling the screen in equal bands, which are the bands the
// touch-to-hear menu works on. A screen reader's player chooses a button as any other (onClick). With none on, a layer over the
// buttons takes the touches instead and the game speaks the options; the one the cursor is on has a frame round it.
@Composable
fun MenuScreen(session: Session) {
    val screen by session.screen.collectAsState()
    val items by session.items.collectAsState()
    val body by session.body.collectAsState()
    val focus by session.focus.collectAsState()
    val readerOn by session.screenReaderOn.collectAsState()
    val said by session.lastLine.collectAsState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // The hand-over line is announced as it appears; the sound tour's lines as each is said.
            body.forEach { Text(it, modifier = if (screen is Screen.HandOver) Polite else Modifier) }
            if (screen == Screen.Learn) Text(said, modifier = Polite)
            items.forEachIndexed { i, text ->
                val frame = if (!readerOn && i == focus) BorderStroke(6.dp, Palette.accent) else null
                Button(
                    onClick = { session.choose(i) },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    border = frame,
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.ground, contentColor = Palette.text),
                ) { Text(text) }
            }
        }
        if (!readerOn) Box(Modifier.fillMaxSize().touchSurface(session))
    }
}
