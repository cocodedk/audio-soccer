package dk.cocode.soccer.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dk.cocode.soccer.core.text.Lang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val LANG = stringPreferencesKey("lang")
private val ENGINE = stringPreferencesKey("engine")

// A file that cannot be parsed is replaced by an empty one, for writes as much as for reads: without it every setLang and
// setEngine would throw from then on.
internal val settingsCorruptionHandler = ReplaceFileCorruptionHandler<Preferences> { emptyPreferences() }

// One DataStore per file for the whole process: this is what keeps a second Settings from opening the same file.
private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = settingsCorruptionHandler,
)

private fun phoneLang(context: Context): Lang =
    if (context.resources.configuration.locales[0].language == "fa") Lang.FA else Lang.EN

// What the player chose, kept in a DataStore. systemLang is what a language not chosen yet reads as.
class Settings internal constructor(
    private val store: DataStore<Preferences>,
    private val systemLang: Lang,
) : Prefs {
    constructor(context: Context) : this(context.settingsStore, phoneLang(context))

    // The corruption handler replaces an unparsable file; any other read error reads as nothing chosen, so a damaged
    // setting never stops the app from starting.
    private val prefs: Flow<Preferences> = store.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    override val lang: Flow<Lang> = prefs
        .map { p -> Lang.entries.firstOrNull { it.name == p[LANG] } ?: systemLang }
        .distinctUntilChanged()

    override val enginePkg: Flow<String?> = prefs.map { it[ENGINE] }.distinctUntilChanged()

    override suspend fun setLang(lang: Lang) {
        store.edit { it[LANG] = lang.name }
    }

    override suspend fun setEngine(pkg: String) {
        store.edit { it[ENGINE] = pkg }
    }
}
