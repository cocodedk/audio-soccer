package dk.cocode.soccer.core.input

// The cursor of the self-spoken menus (prototype/phone/js/menu.js). Like TalkBack's, it stops at the ends: next() on the
// last option stays on it, and the shell says that option again. What a focused or chosen option does is the shell's.
class Menu(private val count: Int) {
    init {
        require(count > 0) { "a menu needs at least one option" }
    }

    var index = 0
        private set

    fun focus(i: Int) {
        index = i.coerceIn(0, count - 1)
    }

    fun next() = focus(index + 1)

    fun prev() = focus(index - 1)
}
