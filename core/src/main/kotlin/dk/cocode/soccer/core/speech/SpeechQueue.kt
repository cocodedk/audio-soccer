package dk.cocode.soccer.core.speech

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

// What speaks the lines: the phone's text-to-speech. onDone is called when the line has ended, on any thread, and
// also for a line that was stopped, so SpeechQueue ignores a report about a line that is no longer the current one.
interface Voice {
    fun speak(text: String, pitch: Float, onDone: () -> Unit)
    fun stop()
}

// The announcer's queue rules, from prototype/js/speech.js: priority (high, normal, low), cut, key replacement and
// rate limit, many, and the wait after which a line is dropped. One line is spoken at a time. say() and tick() belong to
// one thread; only the voice's onDone may come from another. now() is a clock in milliseconds.
// Not ported: the trial's browser workarounds (a pause after a cancel, the 200 ms pump, stuck-line detection,
// the live regions of screen reader mode and its voices).
@OptIn(ExperimentalAtomicApi::class)
class SpeechQueue(private val voice: Voice, private val now: () -> Long) {
    private class Queued(val line: Line, val at: Long)

    private val queue = ArrayDeque<Queued>()
    private val current = AtomicReference<Queued?>(null)
    private val lastSaid = mutableMapOf<String, Long>()

    fun say(line: Line) {
        val t = now()
        val key = line.key
        if (key != null && line.gapMs > 0) {
            val last = lastSaid[key]
            if (last != null && t - last < line.gapMs) return
        }
        val p = line.priority
        // A line outranks a lower one; an outcome (cut) also outranks a line of its own priority.
        fun outranked(other: Line) = other.priority < p || (line.cut && other.priority <= p)
        // What stays ahead of the new line is never lower than it, so the queue is always in speaking order.
        queue.removeAll { outranked(it.line) || (key != null && !line.many && it.line.key == key) }
        val speaking = current.load()
        if (speaking != null && outranked(speaking.line)) {
            current.store(null)
            voice.stop()
        }
        queue.addLast(Queued(line, t))
        tick()
    }

    // Starts the next line once the voice is idle; call it every frame. It also drops lines that waited too long.
    fun tick() {
        if (current.load() != null || queue.isEmpty()) return
        val t = now()
        queue.removeAll { t - it.at > it.line.waitMs }
        val next = queue.removeFirstOrNull() ?: return
        // A line counts as said only once it is spoken: one that expired unsaid starts no rate limit.
        next.line.key?.let { lastSaid[it] = t }
        current.store(next)
        voice.speak(next.line.text, next.line.pitch) { current.compareAndSet(next, null) }
    }

    // Drops every waiting line and stops the one being spoken. Rate limits are kept.
    fun clear() {
        queue.clear()
        current.store(null)
        voice.stop()
    }
}
