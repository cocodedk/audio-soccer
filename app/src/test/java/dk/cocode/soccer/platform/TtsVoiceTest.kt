package dk.cocode.soccer.platform

import android.provider.Settings
import android.speech.tts.TextToSpeech
import dk.cocode.soccer.core.text.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.robolectric.shadows.ShadowTextToSpeech
import org.robolectric.util.ReflectionHelpers
import java.util.Locale

// The engine's state, its choice and its language. How it speaks is in TtsVoiceSpeakTest.
class TtsVoiceTest : TtsTestBase() {
    @Test
    fun startsLoadingAndIsReadyOnlyOnceTheEngineHasStartedItsFirstLine() {
        val voice = voice(ready = false)
        assertEquals(TtsState.LOADING, voice.state.value)
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        idle()
        assertEquals(TtsState.LOADING, voice.state.value) // up, but not yet heard to work
        progress.onStart("some-other-line")
        idle()
        assertEquals(TtsState.LOADING, voice.state.value)
        progress.onStart(lastId())
        idle()
        assertEquals(TtsState.READY, voice.state.value)
    }

    @Test
    fun theFirstLineIsEnglishAtVolumeZero() {
        voice(ready = false)
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        val probe = engine.spoken.single()
        assertEquals(Locale.ENGLISH, engine.currentLanguage)
        assertEquals(0f, probe.params!!.getFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME), 0f)
        assertEquals(TextToSpeech.QUEUE_FLUSH, probe.queueMode)
        assertEquals(emptyList<String>(), speechLog()) // the probe is not one of the lines the player hears
    }

    // A freshly installed eSpeak NG has no voice data until its own app has been opened once.
    @Test
    fun anEngineThatWillNotTakeEnglishIsNoEngineAndSpeaksNothing() {
        val voice = voice(ready = false, english = false)
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        assertEquals(TtsState.NO_ENGINE, voice.state.value)
        assertEquals(emptyList<Spoken>(), engine.spoken.toList())
    }

    @Test
    fun anEngineThatRefusesTheFirstLineIsNoEngine() {
        val voice = voice(ready = false)
        engine.speakResult = TextToSpeech.ERROR
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        assertEquals(TtsState.NO_ENGINE, voice.state.value)
    }

    @Test
    fun anEngineThatFailsTheFirstLineIsNoEngineAtOnce() {
        val voice = voice(ready = false)
        engine.onInitListener.onInit(TextToSpeech.SUCCESS)
        @Suppress("DEPRECATION")
        progress.onError(lastId())
        idle()
        assertEquals(TtsState.NO_ENGINE, voice.state.value)
    }

    @Test
    fun noEngineReportsNoEngine() {
        val voice = voice(ready = false)
        engine.onInitListener.onInit(TextToSpeech.ERROR)
        assertEquals(TtsState.NO_ENGINE, voice.state.value)
    }

    @Test
    fun anEngineThatFailsInsideTheConstructorReportsNoEngineWithoutThrowing() {
        ManualTtsShadow.initFailsAtOnce = true
        assertEquals(TtsState.NO_ENGINE, voice(ready = false).state.value)
    }

    @Test
    fun noInstalledEngineReportsNoEngine() {
        assertEquals(TtsState.NO_ENGINE, voice(engines = false).state.value)
    }

    @Test
    fun enginesListsTheInstalledOnes() {
        installEngine(app, "com.a.tts", "A Speech")
        installEngine(app, "com.b.tts", "B Speech")
        val engines = voice(engines = false).engines()
        assertEquals(setOf(Engine("com.a.tts", "A Speech"), Engine("com.b.tts", "B Speech")), engines.toSet())
    }

    @Test
    fun theChosenEngineIsRequested() {
        voice("com.example.tts")
        assertEquals("com.example.tts", ReflectionHelpers.getField<String?>(tts, "mRequestedEngine"))
    }

    @Test
    fun noChosenEngineMeansTheSystemDefault() {
        voice()
        assertNull(ReflectionHelpers.getField<String?>(tts, "mRequestedEngine"))
    }

    @Test
    fun theChosenEngineIsTheOneThatSpeaks() {
        assertEquals("com.example.tts", voice("com.example.tts").currentEngine())
    }

    // The platform quietly uses the default when the engine asked for was uninstalled, so asking for it names nothing.
    @Test
    fun anEngineChosenButUninstalledIsNotTheOneThatSpeaks() {
        val voice = voice("gone.engine")
        Settings.Secure.putString(app.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH, "com.example.tts")
        assertEquals("com.example.tts", voice.currentEngine())
    }

    @Test
    fun withNoneChosenTheSystemDefaultSpeaks() {
        val voice = voice()
        Settings.Secure.putString(app.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH, "com.example.tts")
        assertEquals("com.example.tts", voice.currentEngine())
    }

    @Test
    fun missingLanguageFallsBackToEnglish() {
        val voice = voice()
        assertEquals(Lang.EN, voice.useLanguage(Lang.FA))
        assertEquals(Locale.ENGLISH, engine.currentLanguage)
    }

    @Test
    fun persianIsSpokenWhenTheEngineHasIt() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.forLanguageTag("fa-IR"))
        assertEquals(Lang.FA, voice().useLanguage(Lang.FA))
        assertEquals(Locale.forLanguageTag("fa-IR"), engine.currentLanguage)
    }

    @Test
    fun englishStaysEnglishEvenWhenThereIsPersian() {
        ShadowTextToSpeech.addLanguageAvailability(Locale.forLanguageTag("fa"))
        assertEquals(Lang.EN, voice().useLanguage(Lang.EN))
        assertEquals(Locale.ENGLISH, engine.currentLanguage)
    }
}
