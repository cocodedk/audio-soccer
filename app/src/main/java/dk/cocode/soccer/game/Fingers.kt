package dk.cocode.soccer.game

import dk.cocode.soccer.core.input.MenuGestures
import dk.cocode.soccer.core.input.Phase
import dk.cocode.soccer.core.input.Touch
import dk.cocode.soccer.platform.Reader

private const val DEFAULT_HEIGHT_DP = 640f

// The fingers on the screen of a player with no screen reader, and what the screen up does with them: the round takes them for
// steering and tapping, the sound tour ends on a touch, and a menu hears them through the touch-to-hear recognizer, whose bands
// are equal parts of the height it is told (resize). home() leaves the sound tour. nowMs is the clock of Touch.tMs.
internal class Fingers(
    private val stage: Stage,
    private val run: Run,
    private val reader: Reader,
    private val nowMs: () -> Long,
    private val home: () -> Unit,
) {
    private var heightDp = DEFAULT_HEIGHT_DP
    private val menuGestures = MenuGestures({ stage.options.size }, { heightDp })

    fun resize(heightDp: Float) {
        this.heightDp = heightDp
    }

    fun touch(phase: Phase, t: Touch) {
        if (!stage.ready || reader.on.value) return
        when (stage.current) {
            is Screen.Play -> run.touch(phase, t)
            Screen.Learn -> if (phase == Phase.DOWN) home()
            else -> menuGestures.on(phase, t)?.let(stage::menu)
        }
    }

    // Once a frame on a menu: a finger held down explores, a tap with no second one is heard.
    fun tick() {
        if (!reader.on.value) menuGestures.tick(nowMs())?.let(stage::menu)
    }

    fun reset() {
        menuGestures.reset()
        run.reset()
    }
}
