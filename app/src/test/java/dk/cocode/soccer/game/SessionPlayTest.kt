package dk.cocode.soccer.game

import dk.cocode.soccer.core.audio.Sound
import dk.cocode.soccer.core.cues.Buzz
import dk.cocode.soccer.core.input.PlayInput
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.Vec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlayTest {
    private val position = Regex("player x=\\d+\\.\\d y=\\d+\\.\\d")

    @Test
    fun aKickIsLoggedAndHeard() {
        val r = Rig()
        r.start(2)
        r.session.play(PlayInput.KICK)
        r.frames(0.1)
        assertTrue(r.log.contains("event Kick"))
        assertTrue(Sound.KICK in r.sink.plays)
    }

    @Test
    fun twoFingersTappedSayWhereTheBallIs() {
        val r = Rig()
        r.start(2)
        val said = r.spoken.size
        r.session.play(PlayInput.WHERE)
        assertEquals(said + 1, r.spoken.size)
        assertTrue(r.spoken.last().isNotEmpty())
    }

    @Test
    fun theLastLineSaidIsOnTheScreen() {
        val r = Rig()
        r.start(1)
        assertEquals(r.game.roundIntro(1, r.game.levelTask(1)), r.session.lastLine.value)
        assertNotNull(r.session.pitch.value)
    }

    @Test
    fun theirPositionIsLoggedOnceASecond() {
        val r = Rig()
        r.start(2)
        r.frames(3.05)
        assertEquals(3, r.logged("player").size)
        assertTrue(r.logged("player").all { position.matches(it) })
    }

    @Test
    fun steeringRunsAtPlayerSpeed() {
        val r = Rig()
        r.start(2)
        val x = r.player.x
        r.session.steer(Vec(1.0, 0.0))
        r.frames(1.0)
        assertEquals(x + 4.0, r.player.x, 0.2)
        assertTrue(Sound.STEP in r.sink.plays)
    }

    @Test
    fun aLongStallCountsAsAShortFrame() {
        val r = Rig()
        r.start(2)
        val x = r.player.x
        r.session.steer(Vec(1.0, 0.0))
        r.session.frame(30.0)
        assertTrue(r.player.x - x < 1.0)
    }

    @Test
    fun takingTheBallInLevelOneEndsTheRoundAndBuzzes() {
        val r = Rig()
        r.start(1)
        r.runToBall()
        r.frames(0.1)
        assertTrue(r.log.contains("event Take"))
        assertEquals(listOf(Buzz.TAKE), r.haptics.buzzes)
        assertTrue(r.spoken.any { it.contains("Got it") })
        r.frames(3.1)
        assertEquals(2, r.logged("round").size)
        assertEquals(Screen.Play(1), r.screen)
    }

    @Test
    fun aGoalSoundsOnTheFrameThatEndsTheRound() {
        val r = Rig()
        r.start(2)
        for (i in 0 until 60 * 60) {
            // Straight on for the kick (a sideways heading angles it), toward the goal until then.
            if (r.player.x > 28) {
                r.session.steer(Vec(1.0, 0.0))
                r.session.play(PlayInput.KICK)
            } else {
                r.steerToward(GOAL)
            }
            r.frames(1.0 / 60)
            if (r.log.contains("event Goal")) break
        }
        assertTrue(r.log.contains("event Goal"))
        assertTrue(Sound.GOAL in r.sink.plays)
        assertEquals("the won round's fanfare", 1, r.sink.plays.count { it == Sound.FANFARE })
        assertEquals(r.game.goal, r.spoken.last())
    }

    @Test
    fun givingUpEndsTheRoundAndTheThirdOneEndsTheLevel() {
        val r = Rig()
        r.start(1)
        for (round in 1..3) {
            r.pause()
            assertEquals(Screen.Paused(1), r.screen)
            r.choose(r.en.giveUp)
            assertEquals(Screen.Play(1), r.screen)
            assertEquals(r.game.gaveUp, r.spoken.last())
            r.frames(3.1)
        }
        val summary = (r.screen as Screen.Summary).result
        assertEquals(1, summary.level)
        assertEquals(3, summary.rounds.size)
        assertEquals(summary.summary, r.spoken.last())
        assertEquals(listOf(r.en.hearAgain, r.en.backToMenu), r.items)
        assertEquals(listOf(summary.summary), r.session.body.value)
        assertEquals("stop", r.audio.calls.last())
        assertEquals(r.audio.calls.count { it == "start" }, r.audio.calls.count { it == "stop" })
        assertTrue(r.log.contains("screen Summary(level=1)"))
        r.choose(r.en.hearAgain)
        assertEquals(2, r.spoken.count { it == summary.summary })
        r.choose(r.en.backToMenu)
        assertEquals(Screen.Home, r.screen)
    }

    @Test
    fun resumingSaysWhereThePlayerIsAndSoundStartsAgain() {
        val r = Rig()
        r.start(2)
        r.frames(0.5)
        r.pause()
        val silences = r.sink.silences
        r.choose(r.en.resume)
        assertEquals(Screen.Play(2), r.screen)
        assertEquals(listOf("start", "stop", "start"), r.audio.calls)
        assertTrue(r.sink.silences > silences)
        assertTrue(r.spoken.last().startsWith("Round 1"))
    }

    @Test
    fun nothingPlaysWhilePaused() {
        val r = Rig()
        r.start(2)
        r.frames(0.5)
        r.pause()
        val plays = r.sink.plays.size
        val said = r.spoken.size
        r.session.steer(Vec(1.0, 0.0))
        r.frames(2.0)
        assertEquals(plays, r.sink.plays.size)
        assertEquals(said, r.spoken.size)
    }

    @Test
    fun backToMenuFromThePauseMenuLeavesTheLevel() {
        val r = Rig()
        r.start(1)
        r.pause()
        r.choose(r.en.backToMenu)
        assertEquals(Screen.Home, r.screen)
        r.frames(1.0)
        assertEquals(1, r.logged("round").size)
    }
}
