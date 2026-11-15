package dk.cocode.soccer.platform

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowTextToSpeech
import org.robolectric.util.ReflectionHelpers

class Spoken(val text: String, val queueMode: Int, val id: String?)

// A speech engine that reports nothing by itself: the test says when a line ends, fails or is cut off, so each of those
// paths is exercised alone (Robolectric's own shadow completes every line the moment the main looper idles).
// speakResult is what speak() returns; a refused line is not recorded. With initFailsAtOnce the engine tells the init
// listener ERROR from inside the constructor, as the real one does when the engine is not installed.
@Implements(TextToSpeech::class)
class ManualTtsShadow : ShadowTextToSpeech() {
    val spoken = mutableListOf<Spoken>()
    var speakResult = TextToSpeech.SUCCESS

    // The stock shadow has not stored the init listener yet when initTts runs, so the real engine, which has, is asked.
    @RealObject
    private lateinit var engine: TextToSpeech

    @Implementation
    override fun initTts(): Int {
        if (initFailsAtOnce) {
            ReflectionHelpers.callInstanceMethod<Any>(engine, "dispatchOnInit", ReflectionHelpers.ClassParameter.from(Integer.TYPE, TextToSpeech.ERROR))
        }
        return super.initTts()
    }

    companion object {
        @Volatile
        var initFailsAtOnce = false

        fun reset() {
            initFailsAtOnce = false
        }
    }

    @Implementation
    override fun speak(text: CharSequence, queueMode: Int, params: Bundle?, utteranceId: String?): Int {
        if (speakResult == TextToSpeech.SUCCESS) spoken += Spoken(text.toString(), queueMode, utteranceId)
        return speakResult
    }
}

// Makes a speech engine visible to TextToSpeech.getEngines(). The non-deprecated way, addOrUpdateService with an intent
// filter, needs the package installed in the shadow package manager first: more ceremony than a fake engine is worth.
@Suppress("DEPRECATION")
fun installEngine(app: Application, pkg: String, label: String) {
    val service = ServiceInfo().apply {
        packageName = pkg
        name = "$pkg.TtsService"
        nonLocalizedLabel = label
        applicationInfo = ApplicationInfo().apply { packageName = pkg }
    }
    val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
    shadowOf(app.packageManager).addResolveInfoForIntent(intent, ResolveInfo().apply { serviceInfo = service })
}

// What the TtsVoice tests share: a phone with one speech engine installed, and the engine's side of the conversation.
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ManualTtsShadow::class])
abstract class TtsTestBase {
    protected val app: Application = RuntimeEnvironment.getApplication()
    protected val tts: TextToSpeech get() = ShadowTextToSpeech.getLastTextToSpeechInstance()
    protected val engine: ManualTtsShadow get() = Shadow.extract(tts)
    protected val progress: UtteranceProgressListener get() = engine.utteranceProgressListener

    // What TextToSpeech passes along with every line: the pitch and, if one was ever set, the speech rate.
    protected val params: Bundle get() = ReflectionHelpers.getField(tts, "mParams")

    @After
    fun resetTtsShadow() = ManualTtsShadow.reset()

    // How many times the onDone of a line from say() has run.
    protected var done = 0

    // A voice on a phone with an engine installed, unless engines is false; ready: the engine finished starting.
    protected fun voice(enginePkg: String? = null, engines: Boolean = true, ready: Boolean = true): TtsVoice {
        if (engines) installEngine(app, "com.example.tts", "Example Speech")
        return TtsVoice(app, enginePkg).also { if (ready) engine.onInitListener.onInit(TextToSpeech.SUCCESS) }
    }

    protected fun idle() = shadowOf(Looper.getMainLooper()).idle()

    protected fun TtsVoice.say(text: String = "line", pitch: Float = 1f) = speak(text, pitch) { done++ }

    protected fun lastId() = engine.spoken.last().id!!

    // The lines logged to logcat for the e2e script.
    protected fun speechLog() = ShadowLog.getLogsForTag("SoccerSpeech").map { it.msg }
}
