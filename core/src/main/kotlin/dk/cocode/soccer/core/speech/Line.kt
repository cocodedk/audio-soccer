package dk.cocode.soccer.core.speech

// How urgent a line is: a higher line cuts off a lower one that is being spoken, and drops the lower ones that wait.
enum class Priority { LOW, NORMAL, HIGH }

// One thing to say, with the rules the trial's say() took as options (prototype/js/speech.js).
// cut: an outcome ("Goal!", "Saved!") also cuts off the line being spoken, and drops the waiting ones, at its own
//   priority, so it is never left behind a long line (an answer to "where", the round intro) until it expires.
// key + gapMs: rate-limit a kind of line, measured from when a line with that key was last spoken. A newer line with
//   the same key replaces one still waiting, unless many is set (a sequence of lines sharing one key).
// waitMs: how long the line may sit unsaid. A line still waiting after that is dropped, never said late.
// onSpoken: called when the line starts being spoken, once: not when it is queued, and never for a line that is dropped,
//   cut off or replaced before it starts. For a caller that must know a line was really said, not merely offered.
data class Line(
    val text: String,
    val priority: Priority = Priority.NORMAL,
    val cut: Boolean = false,
    val key: String? = null,
    val gapMs: Long = 0,
    val waitMs: Long = 1500,
    val many: Boolean = false,
    val pitch: Float = 1f,
    val onSpoken: (() -> Unit)? = null,
)
