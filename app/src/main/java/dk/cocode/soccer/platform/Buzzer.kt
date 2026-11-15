package dk.cocode.soccer.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import dk.cocode.soccer.core.cues.Buzz

// Plays a Buzz on the phone's vibrator, once. A phone with no vibrator does nothing.
class Buzzer(context: Context) : Haptics {
    private val vibrator = context.applicationContext.getSystemService(Vibrator::class.java)?.takeIf { it.hasVibrator() }

    override fun buzz(b: Buzz) {
        val vibrator = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(b.pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(b.pattern, -1)
        }
    }
}
