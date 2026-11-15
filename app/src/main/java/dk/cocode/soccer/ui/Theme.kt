package dk.cocode.soccer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// High contrast on black, for a player with low vision as much as for the helper watching the screen.
internal object Palette {
    val ground = Color.Black
    val text = Color.White
    val accent = Color(0xFFFFD60A) // the focused option, the goal
    val lines = Color.White
    val player = Color(0xFF00E5FF)
    val ball = Color(0xFFFF9100)
    val keeper = Color(0xFFFF4081)
}

private val Colors = darkColorScheme(
    primary = Palette.accent,
    onPrimary = Color.Black,
    background = Palette.ground,
    onBackground = Palette.text,
    surface = Palette.ground,
    onSurface = Palette.text,
)

@Composable
fun SoccerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
