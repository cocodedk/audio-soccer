package dk.cocode.soccer.core.sim

import org.junit.jupiter.api.Test

// The keeper tests replay the trial's rounds, so seeded() must give the sequence helpers.mjs gives.
class SeededTest {
    @Test
    fun seededMatchesTheTrialsGenerator() {
        val a = seeded(11)
        close(a.nextDouble(), 0.5115870486479253)
        close(a.nextDouble(), 0.5299464082345366)
        close(a.nextDouble(), 0.6081185641232878)
        val b = seeded(1)
        close(b.nextDouble(), 0.6270739405881613)
        close(b.nextDouble(), 0.002735721180215478)
    }
}
