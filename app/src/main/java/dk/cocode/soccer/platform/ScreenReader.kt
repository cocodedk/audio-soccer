package dk.cocode.soccer.platform

import android.content.Context
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Whether a screen reader is on: Android's touch exploration, which TalkBack and other screen readers turn on. It lives as
// long as the process, so make one and keep it.
class ScreenReader(context: Context) : Reader {
    private val manager = context.applicationContext.getSystemService(AccessibilityManager::class.java)
    private val mutableOn = MutableStateFlow(manager.isTouchExplorationEnabled)
    override val on: StateFlow<Boolean> = mutableOn.asStateFlow()

    init {
        manager.addTouchExplorationStateChangeListener { mutableOn.value = it }
    }
}
