package dk.cocode.soccer.core.input

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

// The cursor of prototype/phone/js/menu.js: it stops at the ends, so a swipe past the last option stays on it.
class MenuTest {
    @Test
    fun startsOnTheFirstOptionAndNextAndPrevStopAtTheEnds() {
        val m = Menu(3)
        assertEquals(0, m.index)
        m.prev()
        assertEquals(0, m.index)
        m.next()
        m.next()
        assertEquals(2, m.index)
        m.next()
        assertEquals(2, m.index)
        m.prev()
        assertEquals(1, m.index)
    }

    @Test
    fun focusJumpsToAnOptionAndStaysInsideTheMenu() {
        val m = Menu(3)
        m.focus(2)
        assertEquals(2, m.index)
        m.focus(7)
        assertEquals(2, m.index)
        m.focus(-1)
        assertEquals(0, m.index)
    }

    @Test
    fun aMenuOfOneOptionNeverMoves() {
        val m = Menu(1)
        m.next()
        m.prev()
        m.focus(4)
        assertEquals(0, m.index)
    }

    @Test
    fun aMenuNeedsAnOption() {
        assertThrows(IllegalArgumentException::class.java) { Menu(0) }
    }
}
