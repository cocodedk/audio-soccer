package dk.cocode.soccer.platform

import dk.cocode.soccer.core.cues.Buzz
import dk.cocode.soccer.core.game.Records
import dk.cocode.soccer.core.speech.Voice
import dk.cocode.soccer.core.text.Lang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// The phone's side of the game as the game session sees it: one small interface per adapter, so the session can be
// driven with fakes in tests. The adapters hold no game logic.

// A speech engine installed on the phone: its package name and the name the player knows it by.
data class Engine(val pkg: String, val label: String)

// LOADING: the engine is starting, or is up but has not yet been heard to work. READY: it has started a line. NO_ENGINE: there
// is none, it failed to start, it will not speak English, or it was shut down.
enum class TtsState { LOADING, READY, NO_ENGINE }

// The phone's text-to-speech. A line is spoken in the language chosen by useLanguage, once the state is READY.
interface Speech : Voice {
    val state: StateFlow<TtsState>
    fun engines(): List<Engine>

    // The package of the engine that speaks: the one asked for if it is installed, or else the system's default. Null if the phone
    // does not say.
    fun currentEngine(): String?

    // Chooses the language to speak and returns the one the engine will really speak: Persian only if the engine has it,
    // English otherwise. Call it once the state is READY, and with the engine stopped (Talk does): see Talk.speakLanguage.
    fun useLanguage(lang: Lang): Lang
    fun shutdown()
}

// Whether the player has a screen reader on (touch exploration), kept current.
interface Reader {
    val on: StateFlow<Boolean>
}

interface Haptics {
    fun buzz(b: Buzz)
}

// The game's sounds playing: start() opens the output and keeps the mixer rendering into it, stop() closes it.
interface Audio {
    fun start()
    fun stop()
}

// What the player chose, and the records they set. A language not chosen yet follows the phone's; no engine chosen means the
// system's default.
interface Prefs {
    val lang: Flow<Lang>
    val enginePkg: Flow<String?>
    val records: Flow<Records>
    suspend fun setLang(lang: Lang)
    suspend fun setEngine(pkg: String)
    suspend fun setRecords(records: Records)
}
