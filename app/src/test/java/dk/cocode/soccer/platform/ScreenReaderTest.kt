package dk.cocode.soccer.platform

import android.view.accessibility.AccessibilityManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ScreenReaderTest {
    private val app = RuntimeEnvironment.getApplication()
    private val manager = shadowOf(app.getSystemService(AccessibilityManager::class.java))

    @Test
    fun isOffWhileTouchExplorationIsOff() {
        assertFalse(ScreenReader(app).on.value)
    }

    @Test
    fun startsOnWhenTouchExplorationIsAlreadyOn() {
        manager.setTouchExplorationEnabled(true)
        assertTrue(ScreenReader(app).on.value)
    }

    @Test
    fun followsTouchExplorationChanges() {
        val reader = ScreenReader(app)
        manager.setTouchExplorationEnabled(true)
        assertTrue(reader.on.value)
        manager.setTouchExplorationEnabled(false)
        assertFalse(reader.on.value)
    }
}
