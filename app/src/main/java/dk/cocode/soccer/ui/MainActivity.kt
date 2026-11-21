package dk.cocode.soccer.ui

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import dk.cocode.soccer.core.audio.Mixer
import dk.cocode.soccer.game.Session
import dk.cocode.soccer.platform.AudioOut
import dk.cocode.soccer.platform.Buzzer
import dk.cocode.soccer.platform.ScreenReader
import dk.cocode.soccer.platform.Settings
import dk.cocode.soccer.platform.TtsVoice
import kotlin.random.Random

// Builds the session out of the phone's adapters and lets it know when the app comes to the front and leaves it. The rest is
// the Compose screens.
class MainActivity : ComponentActivity() {
    private lateinit var session: Session

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mixer = Mixer(Random.nextLong())
        session = Session(
            speech = { pkg -> TtsVoice(this, pkg) },
            reader = ScreenReader(this),
            haptics = Buzzer(this),
            audio = AudioOut(mixer),
            sink = mixer,
            prefs = Settings(this),
            scope = lifecycleScope,
            nowMs = SystemClock::uptimeMillis,
            rand = Random.Default,
        )
        onBackPressedDispatcher.addCallback(this) { if (!session.back()) finish() }
        setContent { App(session) }
    }

    override fun onResume() {
        super.onResume()
        session.foreground(true)
    }

    override fun onPause() {
        session.foreground(false)
        super.onPause()
    }

    override fun onDestroy() {
        session.close()
        super.onDestroy()
    }
}
