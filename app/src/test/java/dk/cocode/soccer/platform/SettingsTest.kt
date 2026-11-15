package dk.cocode.soccer.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dk.cocode.soccer.core.text.Lang
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    // A store on a file of its own: two stores on one file in one process is an error.
    private fun store(garbage: Boolean = false): DataStore<Preferences> {
        val file = File(tmp.newFolder(), "settings.preferences_pb")
        if (garbage) file.writeBytes(byteArrayOf(-1, -1, -1, -1))
        return PreferenceDataStoreFactory.create { file }
    }

    @Test
    fun langDefaultsToTheSystemLanguage() = runTest {
        assertEquals(Lang.EN, Settings(store(), Lang.EN).lang.first())
        assertEquals(Lang.FA, Settings(store(), Lang.FA).lang.first())
    }

    @Test
    fun engineDefaultsToTheSystemDefault() = runTest {
        assertNull(Settings(store(), Lang.EN).enginePkg.first())
    }

    @Test
    fun theChosenLanguageAndEngineAreKept() = runTest {
        val settings = Settings(store(), Lang.EN)
        settings.setLang(Lang.FA)
        settings.setEngine("com.example.tts")
        assertEquals(Lang.FA, settings.lang.first())
        assertEquals("com.example.tts", settings.enginePkg.first())
    }

    @Test
    fun theChosenLanguageBeatsTheSystemLanguage() = runTest {
        val settings = Settings(store(), Lang.FA)
        settings.setLang(Lang.EN)
        assertEquals(Lang.EN, settings.lang.first())
    }

    @Test
    fun anUnknownStoredLanguageReadsAsTheDefault() = runTest {
        val store = store()
        store.edit { it[stringPreferencesKey("lang")] = "KLINGON" }
        assertEquals(Lang.FA, Settings(store, Lang.FA).lang.first())
    }

    @Test
    fun anUnreadableFileReadsAsTheDefaults() = runTest {
        val settings = Settings(store(garbage = true), Lang.FA)
        assertEquals(Lang.FA, settings.lang.first())
        assertNull(settings.enginePkg.first())
    }

    @Test
    fun theAppStoreStartsInEnglishOnAnEnglishPhone() = runTest {
        assertEquals(Lang.EN, Settings(RuntimeEnvironment.getApplication()).lang.first())
    }

    @Test
    @Config(qualifiers = "fa")
    fun theAppStoreStartsInPersianOnAPersianPhone() = runTest {
        assertEquals(Lang.FA, Settings(RuntimeEnvironment.getApplication()).lang.first())
    }
}
