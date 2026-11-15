package dk.cocode.soccer.game

import dk.cocode.soccer.core.audio.Sound
import dk.cocode.soccer.core.audio.SoundSink
import dk.cocode.soccer.core.cues.Buzz
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.platform.Audio
import dk.cocode.soccer.platform.Engine
import dk.cocode.soccer.platform.Haptics
import dk.cocode.soccer.platform.Prefs
import dk.cocode.soccer.platform.Reader
import dk.cocode.soccer.platform.Speech
import dk.cocode.soccer.platform.TtsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// A speech engine that records what it is told. Each line it is given counts as spoken at once.
class FakeSpeech(
    val pkg: String?,
    initial: TtsState = TtsState.READY,
    private val voices: Set<Lang> = setOf(Lang.EN, Lang.FA),
    private val installed: List<Engine> = emptyList(),
) : Speech {
    override val state = MutableStateFlow(initial)
    val said = mutableListOf<String>()
    var stops = 0
    var shutdowns = 0
    var language: Lang? = null

    override fun engines() = installed

    override fun currentEngine() = pkg ?: installed.firstOrNull()?.pkg

    override fun useLanguage(lang: Lang): Lang = (if (lang in voices) lang else Lang.EN).also { language = it }

    override fun speak(text: String, pitch: Float, onDone: () -> Unit) {
        said += text
        onDone()
    }

    override fun stop() {
        stops += 1
    }

    override fun shutdown() {
        shutdowns += 1
    }
}

class FakeReader(on: Boolean = false) : Reader {
    override val on = MutableStateFlow(on)
}

class FakeHaptics : Haptics {
    val buzzes = mutableListOf<Buzz>()

    override fun buzz(b: Buzz) {
        buzzes += b
    }
}

class FakeAudio : Audio {
    val calls = mutableListOf<String>()

    override fun start() {
        calls += "start"
    }

    override fun stop() {
        calls += "stop"
    }
}

class FakeSink(private val onPlay: (Sound) -> Unit = {}) : SoundSink {
    val plays = mutableListOf<Sound>()
    val behinds = mutableListOf<Boolean>()
    val stretches = mutableListOf<Double>()
    var silences = 0

    override fun play(sound: Sound, gain: Double, behind: Boolean, stretch: Double) {
        plays += sound
        behinds += behind
        stretches += stretch
        onPlay(sound)
    }

    override fun crowd(level: Double) = Unit

    override fun silence() {
        silences += 1
    }
}

class FakePrefs(lang: Lang = Lang.EN, engine: String? = null) : Prefs {
    private val langs = MutableStateFlow(lang)
    private val engines = MutableStateFlow(engine)
    override val lang: Flow<Lang> = langs
    override val enginePkg: Flow<String?> = engines

    fun langNow() = langs.value

    fun enginePkgNow() = engines.value

    override suspend fun setLang(lang: Lang) {
        langs.value = lang
    }

    override suspend fun setEngine(pkg: String) {
        engines.value = pkg
    }
}

val ESPEAK = Engine("org.espeak", "eSpeak NG")
val GOOGLE = Engine("com.google.tts", "Google")
