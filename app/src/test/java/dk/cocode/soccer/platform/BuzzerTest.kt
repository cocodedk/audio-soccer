package dk.cocode.soccer.platform

import android.os.Vibrator
import dk.cocode.soccer.core.cues.Buzz
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class BuzzerTest {
    private val app = RuntimeEnvironment.getApplication()
    private val vibrator = shadowOf(app.getSystemService(Vibrator::class.java))

    private fun assertBuzzesOnce(b: Buzz) {
        Buzzer(app).buzz(b)
        assertArrayEquals(b.pattern, vibrator.pattern)
        assertEquals(-1, vibrator.repeat)
    }

    @Test
    @Config(sdk = [26])
    fun playsTheWaveformFromApi26() = Buzz.entries.forEach { assertBuzzesOnce(it) }

    @Test
    @Config(sdk = [25])
    fun playsThePatternBeforeApi26() = Buzz.entries.forEach { assertBuzzesOnce(it) }

    @Test
    fun doesNothingWhenThePhoneCannotVibrate() {
        vibrator.setHasVibrator(false)
        Buzzer(app).buzz(Buzz.TAKE)
        assertNull(vibrator.pattern)
    }
}
