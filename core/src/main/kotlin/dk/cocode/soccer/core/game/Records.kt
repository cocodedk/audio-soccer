package dk.cocode.soccer.core.game

import dk.cocode.soccer.core.sim.tenthsOf
import dk.cocode.soccer.core.sim.tenthsText
import dk.cocode.soccer.core.text.GameStrings

// What the player has done best, kept on the phone: for each level its fastest won round in whole tenths of a second, the
// precision a time is spoken in, and the levels whose three rounds were all won in one go. The trial has no records.
data class Records(val best: Map<Int, Int> = emptyMap(), val allWon: Set<Int> = emptySet())

// A finished level: its summary line, with "New record: 7.4 seconds." when its best time fell (a level's first win counts)
// and "All three rounds won." when they were, and the records it leaves. Only won rounds count, and a time that sounds the
// same as the best does not beat it.
internal fun levelResult(level: Int, rounds: List<RoundStats>, before: Records, s: GameStrings): LevelResult {
    val fastest = rounds.filter { it.success }.minOfOrNull { tenthsOf(it.time) }
    val newBest = fastest?.takeIf { it < (before.best[level] ?: Int.MAX_VALUE) }
    val allWon = rounds.all { it.success }
    val records = Records(
        best = if (newBest == null) before.best else before.best + (level to newBest),
        allWon = if (allWon) before.allWon + level else before.allWon,
    )
    val news = listOfNotNull(newBest?.let { s.newRecord(tenthsText(it)) }, s.allWon.takeIf { allWon })
    return LevelResult(level, rounds, (listOf(summarySpeech(level, rounds, s)) + news).joinToString(" "), records)
}

// A level's option on the menu: its name, then its records, "Score a goal. Best 7.4 seconds. All three rounds won." Just the
// name while it has none.
fun levelOption(level: Int, name: String, records: Records, s: GameStrings): String {
    val best = records.best[level]?.let { s.bestTime(tenthsText(it)) }
    val extras = listOfNotNull(best, s.allWon.takeIf { level in records.allWon })
    return if (extras.isEmpty()) name else (listOf("$name.") + extras).joinToString(" ")
}
