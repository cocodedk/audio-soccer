package dk.cocode.soccer.core.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// From prototype/test/strings.test.mjs and i18n.test.mjs. What those test of the lookup, of index.html and of the
// level texts has no counterpart here: the interface is the lookup, the app has no page, and Task 4 ports levels.js.
// A ZWNJ (U+200C) is written as the escape, so it can be seen.
class StringsTest {
    private val en = gameStrings(Lang.EN)
    private val fa = gameStrings(Lang.FA)

    // Every member of an interface called with arg for a String and 2 for an Int; a list gives one text per
    // element. Member name and arguments to the text it returns. (Java reflection: the tests run on the JVM.)
    private fun walk(target: Any, type: Class<*>, arg: String): List<Pair<String, String>> =
        type.methods.flatMap { m ->
            val args: List<Any> = m.parameterTypes.map { if (it == String::class.java) arg else 2 }
            val result = m.invoke(target, *args.toTypedArray())
            val texts = (result as? List<*>)?.map { it as String } ?: listOf(result as String)
            assertTrue(texts.isNotEmpty(), "${m.name} is empty")
            texts.mapIndexed { i, text -> "${m.name}$args[$i]" to text }
        }

    private fun gameTexts(lang: Lang, arg: String = "1.5") = walk(gameStrings(lang), GameStrings::class.java, arg)
    private fun uiTexts(lang: Lang, arg: String = "eSpeak") = walk(uiStrings(lang), UiStrings::class.java, arg)

    @Test
    fun everyStringSetInBothLanguages() {
        for (lang in Lang.entries) {
            val all = gameTexts(lang) + uiTexts(lang)
            assertTrue(all.size > 100, "$lang: only ${all.size} strings")
            for ((name, text) in all) assertTrue(text.isNotBlank(), "$lang $name")
        }
    }

    @Test
    fun persianNumbersUsePersianDigits() {
        assertEquals("۱۲ متر", metresText(12.0, fa))
        assertFalse(metresText(12.0, fa).any { it in '0'..'9' })
        for ((name, text) in gameTexts(Lang.FA) + uiTexts(Lang.FA, "موتور")) assertFalse(text.any { it in '0'..'9' }, "$name: $text")
    }

    @Test
    fun digitsTurnADecimalPointBetweenDigitsIntoThePersianSeparator() {
        assertEquals("۴٫۵", fa.num("4.5"))
        assertEquals("۱۲٫۴", fa.num("12.4"))
        assertEquals("۵.", fa.num("5."), "a full stop after a digit stays")
        assertEquals("4.5", en.num("4.5"), "English has no digits to turn")
    }

    @Test
    fun persianTextIsPersianNoEnglishWordsButKeyAndProductNamesZwnjWhereNeeded() {
        val allowed = Regex("Escape|eSpeak|[WGH]\\b")
        val texts = gameTexts(Lang.FA) + uiTexts(Lang.FA, "موتور")
        for ((name, text) in texts) {
            assertFalse(text.replace(allowed, "").any { it in 'A'..'Z' || it in 'a'..'z' }, "$name: Latin letters in \"$text\"")
            assertFalse(Regex("(^|\\s)ن?می ").containsMatchIn(text), "$name: می needs a ZWNJ after it: \"$text\"")
            assertFalse(Regex("\\s(ها|های|تر|ترین)(\\s|$|[.،:؛!؟])").containsMatchIn(text), "$name: a suffix needs a ZWNJ: \"$text\"")
        }
        for (word in listOf("دروازه‌بان", "می‌شود", "یک‌سوم", "نمی‌تواند")) {
            assertTrue(texts.any { it.second.contains(word) }, word)
        }
    }

    @Test
    fun lookupSlotsNumbersAndTheLevelTables() {
        assertEquals("1 metre", en.metres(1))
        assertEquals("3 metres", en.metres(3))
        assertEquals("۳ متر", fa.metres(3))
        assertEquals("Level 2, Score · Round 1 of 3 · 4.5 s", en.statusPlay(2, "Score", 1, 3, "4.5"))
        assertEquals("مرحله ۲، گل زدن · دور ۱ از ۳ · ۴٫۵ ثانیه", fa.statusPlay(2, "گل زدن", 1, 3, "4.5"))
        assertEquals("پشت سر", fa.callBehind)
        assertEquals("شما", fa.canvasYou)
        assertEquals(en.help.size, fa.help.size)
        assertEquals(3, en.thirds.size)
        assertEquals(listOf("Find the ball", "Score", "Beat the keeper"), (1..3).map { en.levelName(it) })
        assertEquals(listOf("پیدا کردن توپ", "گل زدن", "شکست دادن دروازه‌بان"), (1..3).map { fa.levelName(it) })
        assertEquals(listOf("Find the ball.", "Score a goal.", "Beat the keeper."), (1..3).map { en.levelTask(it) })
        assertEquals(listOf("not found", "not scored", "not scored"), (1..3).map { en.levelMiss(it) })
        assertEquals("Round 2. Score a goal.", en.roundIntro(2, en.levelTask(2)))
        assertEquals("دور ۲. گل بزنید.", fa.roundIntro(2, fa.levelTask(2)))
    }

    @Test
    fun theExtraLinesOfRound1ExistForLevels2And3Only() {
        for (s in listOf(en, fa)) {
            assertEquals(emptyList<String>(), s.extra(1))
            assertEquals(1, s.extra(2).size)
            assertEquals(2, s.extra(3).size)
        }
    }

    @Test
    fun summaryPartsFitTogetherInBothLanguages() {
        assertEquals("Level 1 done. Times: 12.4, not found.", en.summaryTimes(en.summaryDone(1), "12.4" + en.summarySep + en.levelMiss(1)))
        assertEquals("Level 1 stopped after 2 of 3 rounds.", en.summaryStopped(1, 2, 3))
        assertEquals("مرحله ۱ بعد از ۲ دور از ۳ دور متوقف شد.", fa.summaryStopped(1, 2, 3))
        assertEquals("مرحله ۱ تمام شد. زمان‌ها: ۱۲٫۴، پیدا نشد.", fa.summaryTimes(fa.summaryDone(1), "12.4" + fa.summarySep + fa.levelMiss(1)))
    }

    @Test
    fun thePersianFallbackSpeaksOfThisDeviceWhichMayStillHaveAnOnlineVoice() {
        assertTrue(Regex("^There is no Persian voice on this device\\.").containsMatchIn(en.langFallback))
        assertTrue(Regex("^این دستگاه صدای فارسی آفلاین ندارد؛").containsMatchIn(fa.langFallback))
    }

    @Test
    fun theClearWindowIs15SecondsInBothLanguages() {
        assertTrue(en.clearConfirm.contains("15 seconds"))
        assertTrue(fa.clearConfirm.contains("۱۵ ثانیه"))
    }

    @Test
    fun theLanguagesNameThemselves() {
        assertEquals("English", gameStrings(Lang.EN).langSet)
        assertEquals("فارسی", gameStrings(Lang.FA).langSet)
        assertEquals("Language: English", uiStrings(Lang.EN).language)
        assertEquals("زبان: فارسی", uiStrings(Lang.FA).language)
    }

    @Test
    fun theLinesTheBriefFixesInEnglish() {
        val ui = uiStrings(Lang.EN)
        assertEquals("Audio Soccer. Touch the screen to hear the options. Double-tap to choose.", ui.greeting)
        assertEquals(listOf("Find the ball", "Score a goal", "Beat the keeper"), ui.levelNames)
        assertEquals("Learn the sounds", ui.learnSounds)
        assertEquals("How to play", ui.howToPlay)
        assertEquals("Settings", ui.settings)
        assertEquals("Voice: eSpeak NG", ui.voiceEngine("eSpeak NG"))
        assertEquals("This phone has no speech engine, so the game cannot speak. Install one, for example eSpeak. After installing it, open the speech engine's own app once, so it can get its voices ready. Then open the game again.", ui.noEngine)
        assertEquals("This voice cannot speak Persian, so the game speaks English.", ui.englishOnly)
        assertEquals("Resume", ui.resume)
        assertEquals("Give up this round", ui.giveUp)
        assertEquals("Back to menu", ui.backToMenu)
        assertEquals("Hear again", ui.hearAgain)
    }

    // A freshly installed engine has no voice data until its own app has been opened once, so the screen says so (the English
    // is checked in full above).
    @Test
    fun thePersianNoEngineTextTellsThePlayerToOpenTheEnginesOwnAppOnce() {
        assertTrue(uiStrings(Lang.FA).noEngine.contains("یک بار برنامه خود موتور گفتار را باز کنید"))
    }

    @Test
    fun theDragIntroIsTwoShortLinesRunStopKickAndTheFingerTapsAreOnlyInHowToPlay() {
        for (lang in Lang.entries) {
            val ui = uiStrings(lang)
            assertEquals(2, ui.dragIntro.size, "$lang")
            assertEquals(6, ui.howToPlayLines.size, "$lang")
        }
        val intro = uiStrings(Lang.EN).dragIntro.joinToString(" ")
        for (word in listOf("drag", "run", "goal", "stop", "kick")) assertTrue(intro.contains(word), word)
        assertFalse(intro.contains("two fingers") || intro.contains("three fingers"))
        val how = uiStrings(Lang.EN).howToPlayLines
        val words = listOf("drag", "Lift your finger to stop", "Tap to kick", "two fingers", "three fingers", "A kick goes straight")
        for ((line, word) in how.zip(words)) assertTrue(line.contains(word), "$word in $line")
        assertTrue(how[4].contains("pause"))
    }

    @Test
    fun theFirstDragIntroLineIsTheTrialsTwoSentencesAndHowToPlayEndsWithItsKickingTip() {
        assertEquals(
            "Put a finger anywhere on the screen and drag it the way you want to run. " +
                "Up the screen is toward the goal. Lift your finger to stop.",
            uiStrings(Lang.EN).dragIntro[0],
        )
        assertEquals(
            "A kick goes straight up the pitch. Line up with the goal, within about twenty metres, and kick.",
            uiStrings(Lang.EN).howToPlayLines[5],
        )
        // the trial's Persian, with its ZWNJ (U+200C) in the same places
        assertEquals(uiStrings(Lang.FA).howToPlayLines[0] + " " + uiStrings(Lang.FA).howToPlayLines[1], uiStrings(Lang.FA).dragIntro[0])
        assertEquals("شوت مستقیم به سمت جلوی زمین می\u200Cرود. با دروازه هم\u200Cراستا شوید، تا حدود بیست متر، و شوت بزنید.", uiStrings(Lang.FA).howToPlayLines[5])
    }
}
