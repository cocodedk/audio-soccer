package dk.cocode.soccer.game

import dk.cocode.soccer.core.game.LevelResult
import dk.cocode.soccer.core.game.Records
import dk.cocode.soccer.core.speech.Priority
import dk.cocode.soccer.core.text.Lang
import dk.cocode.soccer.core.text.uiStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuItemsTest {
    private val ui = uiStrings(Lang.EN)
    private val calls = mutableListOf<String>()
    private val choices = object : Choices {
        override fun startLevel(n: Int) { calls += "level $n" }
        override fun open(screen: Screen) { calls += "open $screen" }
        override fun home() { calls += "home" }
        override fun nextLanguage() { calls += "language" }
        override fun nextEngine() { calls += "engine" }
        override fun resume() { calls += "resume" }
        override fun giveUp() { calls += "giveUp" }
        override fun hearAgain() { calls += "hearAgain" }
    }
    private val result = LevelResult(2, emptyList(), "Level 2 done.", Records())

    private fun options(s: Screen) = optionsFor(s, ui, { "eSpeak NG" }, { n, name -> "$n $name" }, choices)

    private fun choose(s: Screen, i: Int): String {
        calls.clear()
        options(s)[i].choose()
        return calls.single()
    }

    @Test
    fun homeHasTheThreeLevelsThenLearnHelpAndSettings() {
        val o = options(Screen.Home)
        val levels = ui.levelNames.mapIndexed { i, name -> "${i + 1} $name" } // what levelLabel made of each
        assertEquals(levels + listOf(ui.learnSounds, ui.howToPlay, ui.settings), o.map { it.text })
        assertEquals(listOf("level 1", "level 2", "level 3"), (0..2).map { choose(Screen.Home, it) })
        assertEquals(listOf("open Learn", "open Help", "open Settings"), (3..5).map { choose(Screen.Home, it) })
    }

    @Test
    fun settingsStepsTheLanguageAndTheEngineAndGoesBack() {
        assertEquals(listOf(ui.language, "Voice: eSpeak NG", ui.backToMenu), options(Screen.Settings).map { it.text })
        assertEquals(listOf("language", "engine", "home"), (0..2).map { choose(Screen.Settings, it) })
    }

    @Test
    fun pausedHasResumeGiveUpAndBack() {
        assertEquals(listOf(ui.resume, ui.giveUp, ui.backToMenu), options(Screen.Paused(1)).map { it.text })
        assertEquals(listOf("resume", "giveUp", "home"), (0..2).map { choose(Screen.Paused(1), it) })
    }

    @Test
    fun summaryHasHearAgainAndBack() {
        assertEquals(listOf(ui.hearAgain, ui.backToMenu), options(Screen.Summary(result)).map { it.text })
        assertEquals(listOf("hearAgain", "home"), (0..1).map { choose(Screen.Summary(result), it) })
    }

    @Test
    fun thePlainScreensOnlyGoBackAndPlayAndNoEngineHaveNothingToChoose() {
        for (s in listOf(Screen.Help, Screen.Learn, Screen.HandOver(1))) {
            assertEquals(listOf(ui.backToMenu), options(s).map { it.text })
            assertEquals("home", choose(s, 0))
        }
        assertTrue(options(Screen.NoEngine).isEmpty())
        assertTrue(options(Screen.Play(1)).isEmpty())
    }

    @Test
    fun screensShowTheirText() {
        assertEquals(ui.howToPlayLines, bodyFor(Screen.Help, ui))
        assertEquals(listOf(ui.handOver), bodyFor(Screen.HandOver(2), ui))
        assertEquals(listOf(ui.noEngine), bodyFor(Screen.NoEngine, ui))
        assertEquals(listOf("Level 2 done."), bodyFor(Screen.Summary(result), ui))
        assertTrue(bodyFor(Screen.Home, ui).isEmpty())
    }

    @Test
    fun enteringAScreenSaysItsLines() {
        assertEquals(listOf(ui.greeting), entryLines(Screen.Home, ui).map { it.text })
        assertEquals(listOf(ui.paused), entryLines(Screen.Paused(3), ui).map { it.text })
        val help = entryLines(Screen.Help, ui)
        assertEquals(ui.howToPlayLines, help.map { it.text })
        assertTrue(help.all { it.many && it.key == "help" && it.waitMs >= 60000 && it.priority == Priority.HIGH })
        assertTrue(entryLines(Screen.Summary(result), ui).isEmpty())
        assertTrue(entryLines(Screen.Settings, ui).isEmpty())
    }

    @Test
    fun screensAreLoggedByName() {
        assertEquals("Home", Screen.Home.logName)
        assertEquals("Play(level=2)", Screen.Play(2).logName)
        assertEquals("Paused(level=3)", Screen.Paused(3).logName)
        assertEquals("HandOver(level=1)", Screen.HandOver(1).logName)
        assertEquals("Summary(level=2)", Screen.Summary(result).logName)
    }

    @Test
    fun onlyPlayAndLearnMakeSound() {
        assertTrue(Screen.Play(1).makesSound && Screen.Learn.makesSound)
        assertTrue(listOf(Screen.Home, Screen.Paused(1), Screen.Settings, Screen.Help, Screen.NoEngine).none { it.makesSound })
    }
}
