package dk.cocode.soccer.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dk.cocode.soccer.core.text.Lang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private const val SPEECH_LOG = "SoccerSpeech"
private val PERSIAN = listOf(Locale.forLanguageTag("fa-IR"), Locale.forLanguageTag("fa"))

// The phone's TextToSpeech as the game's Voice. enginePkg is the engine to use, null for the system's default. A line
// flushes whatever the engine was still saying, at the pitch of the line, and at the engine's own speech rate: the app
// never sets one. Every line is logged to SoccerSpeech once the engine has taken it.
// onDone is called at most once per speak(), on the main thread, never inside speak() or stop() themselves: when the line
// ends, is stopped or flushed by the next one, or fails, and when it never started (the engine is not ready, or refused
// it). A line whose engine service dies reports nothing; SpeechQueue releases it, and its stop() settles the entry kept
// here. The engine reports from a binder thread, so each report is posted to the main thread.
class TtsVoice(context: Context, private val enginePkg: String?) : Speech {
    private val main = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(TtsState.LOADING)
    override val state: StateFlow<TtsState> = mutableState.asStateFlow()

    // The onDone of each line handed over and not yet reported, by utterance id. Whoever removes an entry fires it, so a
    // line the engine reports twice, or that stop() ended before the engine did, still fires once.
    private val pending = ConcurrentHashMap<String, () -> Unit>()
    private val lastId = AtomicInteger()

    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) = finish(utteranceId)

        // The engine calls onError(id, code) from API 21, whose default is this one.
        @Suppress("OVERRIDE_DEPRECATION")
        override fun onError(utteranceId: String?) = finish(utteranceId)

        override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
    }

    // A failed start may call back from inside this constructor, before tts exists: onInit leaves tts alone then.
    private val tts = TextToSpeech(context.applicationContext, { status -> onInit(status) }, enginePkg)

    init {
        tts.setOnUtteranceProgressListener(progress)
    }

    private fun onInit(status: Int) {
        mutableState.value =
            if (status == TextToSpeech.SUCCESS && tts.engines.isNotEmpty()) TtsState.READY else TtsState.NO_ENGINE
    }

    override fun engines(): List<Engine> = tts.engines.map { Engine(it.name, it.label) }

    override fun currentEngine(): String? = enginePkg ?: tts.defaultEngine

    override fun useLanguage(lang: Lang): Lang {
        if (lang == Lang.FA && PERSIAN.any { tts.setLanguage(it) >= TextToSpeech.LANG_AVAILABLE }) return Lang.FA
        tts.setLanguage(Locale.ENGLISH)
        return Lang.EN
    }

    override fun speak(text: String, pitch: Float, onDone: () -> Unit) {
        // QUEUE_FLUSH drops what the engine was still saying, and that line's end may never be reported.
        finishAll()
        val id = "soccer-${lastId.incrementAndGet()}"
        pending[id] = onDone
        if (state.value != TtsState.READY) return finish(id)
        tts.setPitch(pitch)
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (result == TextToSpeech.SUCCESS) Log.d(SPEECH_LOG, text) else finish(id)
    }

    override fun stop() {
        tts.stop()
        finishAll()
    }

    override fun shutdown() {
        tts.shutdown()
        finishAll()
    }

    private fun finish(id: String?) {
        if (id != null) main.post { pending.remove(id)?.invoke() }
    }

    private fun finishAll() = pending.keys.forEach(::finish)
}
