package dk.cocode.soccer.game

import dk.cocode.soccer.core.input.Phase
import dk.cocode.soccer.core.text.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// The drag intro: said once per run, before round 1 of the first level the player starts.
class SessionIntroTest {
    private fun Rig.introSaid() = spoken.count { it == en.dragIntro[0] }

    @Test
    fun theFirstLevelBeginsOnceBothLinesOfTheIntroHaveBeenSaid() {
        val r = Rig().holding()
        r.choose(r.en.levelNames[0])
        assertEquals(Screen.Play(1), r.screen)
        assertEquals(listOf(r.en.greeting, r.en.dragIntro[0]), r.spoken)
        r.frames(1.0)
        assertNull(r.session.pitch.value)
        assertTrue(r.logged("round").isEmpty())
        r.speech.release()
        r.frames(0.1)
        assertEquals(listOf(r.en.greeting) + r.en.dragIntro, r.spoken)
        assertNull(r.session.pitch.value) // the second line is still being said
        r.speech.release()
        r.frames(0.1)
        assertEquals(listOf("round 1 level=1"), r.logged("round"))
        assertEquals(r.game.roundIntro(1, r.game.levelTask(1)), r.spoken.last())
    }

    @Test
    fun aFingerDuringTheIntroDoesNotActOnTheGameWhenItBegins() {
        val r = Rig().holding()
        r.choose(r.en.levelNames[0])
        r.touch(Phase.DOWN, 1, 100f, 400f)
        r.touch(Phase.MOVE, 1, 100f, 300f)
        r.tap(2, 200f, 300f)
        r.speech.release()
        r.frames(0.1)
        r.speech.release()
        r.frames(0.1)
        val start = r.player
        r.touch(Phase.MOVE, 1, 100f, 250f) // the finger that was down all along moves on
        r.frames(1.0)
        assertEquals(start, r.player)
        assertTrue(r.logged("event Kick").isEmpty())
    }

    @Test
    fun laterLevelsOfTheRunGoStraightToTheRound() {
        val r = Rig()
        r.start(1)
        r.session.back()
        r.choose(r.en.backToMenu)
        r.choose(r.en.levelNames[1])
        assertEquals(listOf("round 1 level=1", "round 1 level=2"), r.logged("round"))
        assertEquals(1, r.introSaid())
    }

    @Test
    fun backDuringTheIntroLeavesForTheMenuAndTheIntroIsSaidAgain() {
        val r = Rig().holding()
        r.choose(r.en.levelNames[0])
        assertTrue(r.session.back())
        assertEquals(Screen.Home, r.screen)
        r.frames(1.0)
        assertTrue(r.logged("round").isEmpty())
        r.speech.hold = false
        r.start(1)
        assertEquals(2, r.introSaid())
        assertEquals(listOf("round 1 level=1"), r.logged("round"))
    }

    @Test
    fun theBackgroundDuringTheIntroLeavesForTheMenuNotThePauseMenu() {
        val r = Rig().holding()
        r.choose(r.en.levelNames[0])
        r.session.foreground(false)
        assertEquals(Screen.Home, r.screen)
        r.session.foreground(true)
        r.speech.hold = false
        r.start(1)
        assertEquals(2, r.introSaid())
    }

    @Test
    fun withTheScreenReaderOnTheIntroWaitsForTheHandOverToo() {
        val r = Rig(readerOn = true)
        r.start(1)
        assertEquals(Screen.HandOver(1), r.screen)
        r.reader.on.value = false
        r.beginRound()
        assertEquals(r.en.dragIntro + r.game.roundIntro(1, r.game.levelTask(1)), r.spoken)
        assertEquals(listOf("round 1 level=1"), r.logged("round"))
    }

    @Test
    fun theIntroIsSaidInThePersianThePlayerChose() {
        val r = Rig(lang = Lang.FA)
        r.session.choose(0)
        r.beginRound()
        assertEquals(r.fa.dragIntro, r.spoken.takeLast(3).take(2))
        assertEquals(listOf("round 1 level=1"), r.logged("round"))
    }
}
