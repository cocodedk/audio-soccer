package dk.cocode.soccer.core.audio

import dk.cocode.soccer.core.audio.Sound.BOARD
import dk.cocode.soccer.core.audio.Sound.BRUSH
import dk.cocode.soccer.core.audio.Sound.CHIME
import dk.cocode.soccer.core.audio.Sound.END_WALL
import dk.cocode.soccer.core.audio.Sound.FANFARE
import dk.cocode.soccer.core.audio.Sound.GOAL
import dk.cocode.soccer.core.audio.Sound.KEEPER_STEP
import dk.cocode.soccer.core.audio.Sound.KICK
import dk.cocode.soccer.core.audio.Sound.POST
import dk.cocode.soccer.core.audio.Sound.RATTLE
import dk.cocode.soccer.core.audio.Sound.SAVE
import dk.cocode.soccer.core.audio.Sound.STEP
import dk.cocode.soccer.core.audio.Sound.TAP
import dk.cocode.soccer.core.audio.Sound.TICK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The recipes of prototype/js/sfx.js: how long each rings (its longest voice, from the trial's numbers) and what it
// is made of.
class SfxTest {
    private val lengths = mapOf(
        TICK to 0.06, TAP to 0.07, KICK to 0.13, BOARD to 0.1, END_WALL to 0.18, BRUSH to 0.13, STEP to 0.05,
        KEEPER_STEP to 0.05, CHIME to 0.92, POST to 1.0, SAVE to 0.12, GOAL to 2.8, FANFARE to 1.24,
    )

    @Test
    fun everyRecipeRingsAsLongAsTheTrialsLongestVoice() {
        assertEquals(Sound.entries.toSet() - RATTLE, lengths.keys)
        for ((sound, seconds) in lengths) {
            val length = playOnce(sound, 4.0).lastSoundSeconds()
            assertTrue(length in seconds - 0.02..seconds + 0.005, "$sound rings $length s, the trial's takes $seconds s")
        }
    }

    @Test
    fun aRattleIsTwoOrThreeHighGrainsAndOneLowOneWithinFiftyMilliseconds() {
        val counts = (1L..40L).map { seed ->
            val length = playOnce(RATTLE, 0.1, seed = seed).lastSoundSeconds()
            assertTrue(length <= 0.05, "seed $seed rattles for $length s")
            val mixer = Mixer(seed)
            mixer.play(RATTLE)
            mixer.renderSeconds(0.001) // every grain has been born, none has ended
            mixer.voiceCount
        }
        assertEquals(setOf(3, 4), counts.toSet())
    }

    @Test
    fun aRattleIsMetallicWithAMidRangeGrain() {
        val out = (1L..10L).map { playOnce(RATTLE, 0.1, seed = it) }
        for (grains in out) {
            val high = (42..58).sumOf { grains.energyAt(it * 100.0) }
            assertTrue(high > grains.energyAt(200.0) * 10, "the high grains ring above 4 kHz")
        }
        val mid = out.sumOf { grains -> (12..15).sumOf { grains.energyAt(it * 100.0) } }
        assertTrue(mid > out.sumOf { it.energyAt(200.0) } * 10, "the low grain sits at 1.2 to 1.5 kHz")
    }

    @Test
    fun theChimeIsTwoNotesTheSecondOneLater() {
        val out = playOnce(CHIME, 1.0)
        val first = out.between(0.0, 0.15)
        val both = out.between(0.3, 0.45)
        assertTrue(first.energyAt(784.0) > 20 * first.energyAt(1175.0), "only the first note at the start")
        assertTrue(both.energyAt(1175.0) > both.energyAt(784.0) / 20, "the second note has come in")
    }

    @Test
    fun theChimeIsShortenedByStretchWithinTheTrialsLimits() {
        val whole = playOnce(CHIME, 2.0).lastSoundSeconds()
        assertEquals(0.5 * whole, playOnce(CHIME, 2.0, stretch = 0.5).lastSoundSeconds(), 0.03)
        assertEquals(whole, playOnce(CHIME, 2.0, stretch = 3.0).lastSoundSeconds(), 0.001)
        assertEquals(0.35 * whole, playOnce(CHIME, 2.0, stretch = 0.1).lastSoundSeconds(), 0.03)
    }

    @Test
    fun aGoalIsANetThenAWhistleOverACheer() {
        val out = playOnce(GOAL, 4.0)
        assertTrue(out.between(0.0, 0.3).bandEnergy(3500.0, 4500.0) > 3 * out.between(1.0, 1.3).bandEnergy(3500.0, 4500.0), "the net's hiss")
        val whistle = out.between(0.05, 0.3)
        val after = out.between(0.45, 0.7)
        assertTrue(whistle.energyAt(2900.0) + whistle.energyAt(3060.0) > 3 * (after.energyAt(2900.0) + after.energyAt(3060.0)), "the whistle")
        assertTrue(out.between(2.4, 2.6).rms() > 0, "the cheer is still going at 2.5 s")
        val cheer = out.between(1.0, 2.0)
        assertTrue(cheer.bandEnergy(900.0, 1300.0) > 3 * cheer.bandEnergy(100.0, 300.0), "a cheer sits near 1.1 kHz")
    }

    @Test
    fun theFanfareRisesToAChordAndIsGoneByItsEnd() {
        val out = playOnce(FANFARE, 2.0)
        val first = out.between(0.01, 0.1)
        assertTrue(first.energyAt(392.0) > 20 * first.energyAt(784.0), "it starts on its lowest note")
        val chord = out.between(0.5, 0.7)
        assertTrue(chord.energyAt(1047.0) > 20 * chord.energyAt(392.0), "and ends on the chord, the run gone")
        val mixer = Mixer(1L)
        mixer.play(FANFARE)
        mixer.renderSeconds(1.25)
        assertEquals(0, mixer.voiceCount, "every voice has ended")
    }

    @Test
    fun theKeepersStepSoundsLikeAStep() {
        assertTrue(playOnce(STEP, 0.1).contentEquals(playOnce(KEEPER_STEP, 0.1)))
    }

    // The wall's noise sits at 330 Hz and the board's at 750 Hz: the wall has the lower balance.
    @Test
    fun anEndWallIsLowerAndHeavierThanABoard() {
        fun lowOverHigh(sound: Sound) = playOnce(sound, 0.2).let { it.bandEnergy(280.0, 380.0) / it.bandEnergy(650.0, 850.0) }
        assertTrue(lowOverHigh(END_WALL) > 2 * lowOverHigh(BOARD))
    }

    @Test
    fun aPostRingsLongWithItsPartialsFadingOneByOne() {
        val out = playOnce(POST, 1.2)
        assertTrue(out.between(0.9, 1.0).energyAt(523.0) > 0, "the lowest partial rings for the full second")
        assertTrue(out.between(0.9, 1.0).energyAt(3011.0) < 1e-6 * out.between(0.0, 0.1).energyAt(3011.0), "the highest is gone by then")
    }
}
