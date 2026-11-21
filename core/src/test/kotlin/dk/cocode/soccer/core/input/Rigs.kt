package dk.cocode.soccer.core.input

// Feeds a recognizer the way the shell will: a finger id, dp positions and a clock in ms. What it returns is kept in got.
internal abstract class Rig<I : Any> {
    val got = mutableListOf<I>()

    protected abstract fun onTouch(phase: Phase, t: Touch): I?
    protected abstract fun onTick(nowMs: Long): I?
    abstract fun reset()

    private fun touch(phase: Phase, id: Int, x: Int, y: Int, t: Long) {
        onTouch(phase, Touch(id, x.toFloat(), y.toFloat(), t))?.let(got::add)
    }

    fun down(id: Int, x: Int, y: Int, t: Long) = touch(Phase.DOWN, id, x, y, t)
    fun move(id: Int, x: Int, y: Int, t: Long) = touch(Phase.MOVE, id, x, y, t)
    fun up(id: Int, x: Int, y: Int, t: Long) = touch(Phase.UP, id, x, y, t)
    fun cancel(id: Int, t: Long) = touch(Phase.CANCEL, id, 0, 0, t)
    fun tick(t: Long) {
        onTick(t)?.let(got::add)
    }
}

internal class PlayRig : Rig<PlayInput>() {
    private val g = PlayGestures()
    val dir get() = g.dir

    override fun onTouch(phase: Phase, t: Touch) = g.on(phase, t)
    override fun onTick(nowMs: Long) = g.tick(nowMs)
    override fun reset() = g.reset()

    // A short tap of one finger at (x, y) from t to t + ms.
    fun tap(id: Int, t: Long, x: Int = 200, y: Int = 400, ms: Long = 80) {
        down(id, x, y, t)
        up(id, x + 2, y + 1, t + ms)
    }
}

// The default menu is 3 options over 900 dp: band 0 is y 0..300, band 1 300..600, band 2 600..900. count and height can
// change between events, as the lambdas the shell passes are read each time.
internal class MenuRig(var count: Int = 3, var height: Float = 900f) : Rig<MenuInput>() {
    private val g = MenuGestures({ count }, { height })

    override fun onTouch(phase: Phase, t: Touch) = g.on(phase, t)
    override fun onTick(nowMs: Long) = g.tick(nowMs)
    override fun reset() = g.reset()

    // A short tap of one finger at height y from t to t + ms.
    fun tap(id: Int, t: Long, y: Int, ms: Long = 80) {
        down(id, 200, y, t)
        up(id, 202, y + 1, t + ms)
    }

    // A finger held still at height y from t, long enough to explore.
    fun explore(id: Int, t: Long, y: Int) {
        down(id, 200, y, t)
        tick(t + TAP_MS + 1)
    }
}
