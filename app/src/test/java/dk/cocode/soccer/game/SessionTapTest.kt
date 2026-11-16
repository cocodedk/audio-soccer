package dk.cocode.soccer.game

import dk.cocode.soccer.core.input.MenuInput
import org.junit.Assert.assertEquals
import org.junit.Test

// With the screen reader off, one tap anywhere says again the option the cursor is on (owner, 2026-11-16): a player who
// moves with swipes never has to know where on the screen an option sits.
class SessionTapTest {
    @Test
    fun aTapOnItsOwnRepeatsTheOptionTheCursorIsOnWhereverItLands() {
        val r = Rig()
        r.session.menu(MenuInput.Next) // the cursor on the second option
        r.tap(1, 50f, 700f) // on the strip of another option
        r.frames(0.5) // and no second tap comes
        assertEquals(1, r.session.focus.value)
        assertEquals(listOf(r.en.greeting, r.en.levelNames[1], r.en.levelNames[1]), r.spoken)
    }
}
