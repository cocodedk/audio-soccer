package dk.cocode.soccer.core.audio

import dk.cocode.soccer.core.cues.BEHIND
import dk.cocode.soccer.core.cues.beaconEvery
import dk.cocode.soccer.core.cues.distanceGain
import dk.cocode.soccer.core.cues.nearBoard
import dk.cocode.soccer.core.sim.Event
import dk.cocode.soccer.core.sim.GOAL
import dk.cocode.soccer.core.sim.PITCH_W
import dk.cocode.soccer.core.sim.Side
import dk.cocode.soccer.core.sim.THIRD
import dk.cocode.soccer.core.sim.Vec
import dk.cocode.soccer.core.sim.World
import dk.cocode.soccer.core.sim.clamp
import dk.cocode.soccer.core.sim.dist
import kotlin.math.hypot
import kotlin.math.min

// Gains the game plays its sounds at: Soundscape in play, and the sound tour of the app's "Learn the sounds", so what is taught is
// what is heard.
const val TICK_GAIN = 1.2 // a still ball's tick
const val TICK_BEHIND_MAKEUP = 1.3 // what the dull filter takes from a tick behind the player
const val STEP_GAIN = 0.22
const val CHIME_GAIN = 0.4
const val CHIME_FULL_BEAT = 2.0 // s: the goal chime at its slowest beat is played at its full length, a quicker beat shortens it

private const val STEP_EVERY = 0.35
private const val KEEPER_STEP_EVERY = 0.42
private const val ROLLING = 0.3 // m/s: a ball faster than this rattles, a slower one counts as still and ticks

// Seconds between the ticks of a still ball: 0.8 at 20 m or more, 0.2 within 3 m, linear between, so a player closing in hears
// the ticks speed up (at 4 m/s a fixed 0.8 s would be one tick per 3.2 m).
private fun tickEvery(d: Double): Double = clamp(0.2 + (d - 3) * 0.6 / 17, 0.2, 0.8)

// Turns the world into sound each frame (ball rattle or tick, footsteps, board brush, goal chime, keeper steps, crowd swell)
// and plays the sound of each world event. From prototype/js/soundscape.js, speakers path only: nothing is panned, so a sound's
// gain is the trial's own gain times distanceGain of how far it is from the player, fixed when it is triggered. A ball more
// than BEHIND m behind the player plays dull (behind), with a makeup gain for what the lowpass takes from the rattle and the
// tick. Everything goes to the sink; the sink does the rendering, and ramps the crowd by itself.
class Soundscape(private val sink: SoundSink) {
    private var level = 1
    private var step = 0.0
    private var grain = 0.0
    private var tick = 0.0
    private var beacon = 0.0
    private var keeperStep = 0.0
    private var hot: Boolean? = null

    fun reset(level: Int) {
        this.level = level
        step = 0.0
        grain = 0.0
        tick = 0.3
        beacon = 0.6
        keeperStep = 0.0
        hot = null
    }

    // dt: seconds since the last frame; events: what happened in it. The events go last, so a goal's crowd swell is not undone
    // by the crowd level of the same frame.
    fun update(w: World, dt: Double, events: List<Event>) {
        ball(w, dt)
        feet(w, dt)
        chime(w, dt)
        keeper(w, dt)
        crowd(w)
        for (e in events) event(e, w)
    }

    // A sound at `at`, heard by the player: the distance fixes its gain.
    private fun spatial(sound: Sound, w: World, at: Vec, gain: Double, behind: Boolean = false, stretch: Double = 1.0) {
        sink.play(sound, gain * distanceGain(dist(w.player, at)), behind, stretch)
    }

    // A sound of the ball: dull when it is behind the player, with the makeup that gives back what the lowpass takes.
    private fun ballSound(sound: Sound, w: World, at: Vec, gain: Double, makeup: Double = 1.0) {
        val behind = at.x - w.player.x < -BEHIND
        spatial(sound, w, at, if (behind) gain * makeup else gain, behind)
    }

    private fun ball(w: World, dt: Double) {
        val b = w.ball
        val speed = hypot(w.ballVel.x, w.ballVel.y)
        val every = tickEvery(dist(w.player, b))
        if (speed > ROLLING) {
            grain -= dt
            if (grain <= 0) {
                grain = clamp(0.16 - speed * 0.009, 0.035, 0.16)
                ballSound(Sound.RATTLE, w, b, 0.45 + min(speed / 14, 1.0) * 0.55, makeup = 2.0)
            }
            tick = every
        } else {
            tick = min(tick - dt, every) // closing in shortens the wait at once
            if (tick <= 0) {
                tick = every
                ballSound(Sound.TICK, w, b, TICK_GAIN, makeup = TICK_BEHIND_MAKEUP)
            }
        }
    }

    private fun feet(w: World, dt: Double) {
        val p = w.player
        if (!w.playerMoving) {
            step = 0.05
            return
        }
        step -= dt
        if (step > 0) return
        step = STEP_EVERY
        sink.play(Sound.STEP, STEP_GAIN) // a footstep is at the player's own feet
        nearBoard(p)?.let { spatial(Sound.BRUSH, w, Vec(p.x, if (it == Side.LEFT) 0.0 else PITCH_W), 0.7) }
    }

    // Only while you have the ball: then the goal is your target. Soft, so the ball stays the clearest sound. The wait shrinks
    // as you close in on the goal (at once, like the still ball's tick), and the chime is shortened so the quick ones do not
    // run into each other.
    private fun chime(w: World, dt: Double) {
        if (!w.hasBall) {
            beacon = min(beacon, 0.3)
            return
        }
        val every = beaconEvery(dist(w.player, GOAL))
        beacon = min(beacon - dt, every)
        if (beacon <= 0) {
            beacon = every
            spatial(Sound.CHIME, w, GOAL, CHIME_GAIN, stretch = every / CHIME_FULL_BEAT)
        }
    }

    private fun keeper(w: World, dt: Double) {
        val k = w.keeper ?: return
        if (!k.moving) return
        keeperStep -= dt
        if (keeperStep <= 0) {
            keeperStep = KEEPER_STEP_EVERY
            spatial(Sound.KEEPER_STEP, w, k.pos, 0.7)
        }
    }

    // A gentle swell only: the crowd must never cover the ball's tick. No bed in level 1.
    private fun crowd(w: World) {
        val nowHot = w.ball.x > 2 * THIRD
        if (nowHot == hot) return
        hot = nowHot
        sink.crowd(if (level == 1) 0.0 else if (nowHot) 0.07 else 0.045)
    }

    private fun event(e: Event, w: World) {
        when (e) {
            is Event.Take -> ballSound(Sound.TAP, w, Vec(e.x, e.y), 0.7)
            is Event.Tap -> ballSound(Sound.TAP, w, Vec(e.x, e.y), 0.7)
            is Event.Kick -> ballSound(Sound.KICK, w, Vec(e.x, e.y), 1.0)
            is Event.Bump -> spatial(if (e.side == Side.END) Sound.END_WALL else Sound.BOARD, w, Vec(e.x, e.y), 0.8)
            is Event.Board -> ballSound(Sound.BOARD, w, Vec(e.x, e.y), force(e.speed))
            is Event.EndWall -> ballSound(Sound.END_WALL, w, Vec(e.x, e.y), force(e.speed))
            is Event.Post -> spatial(Sound.POST, w, Vec(e.x, e.y), 1.0)
            is Event.Save -> spatial(Sound.SAVE, w, Vec(e.x, e.y), 1.0)
            // The cheer and the whistle must not dim with distance, so no distance gain here.
            is Event.Goal -> {
                sink.play(Sound.GOAL)
                sink.crowd(0.3)
            }
            is Event.Voy, Event.NoKick -> Unit // the keeper's voy is spoken (Game), not played
        }
    }

    private fun force(speed: Double) = clamp(speed / 8, 0.25, 1.0)
}
