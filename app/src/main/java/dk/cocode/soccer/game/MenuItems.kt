package dk.cocode.soccer.game

import dk.cocode.soccer.core.speech.Line
import dk.cocode.soccer.core.speech.Priority
import dk.cocode.soccer.core.text.UiStrings

// How long a line of a menu may wait behind the one before it: a menu line is never dropped for being late.
internal const val MENU_WAIT_MS = 20000L

// How long one of the how-to-play lines may wait: all of them are queued at once and spoken in turn.
private const val HELP_WAIT_MS = 120000L

internal class Option(val text: String, val choose: () -> Unit)

// What choosing an option does: the session's side of the menus.
internal interface Choices {
    fun startLevel(n: Int)
    fun open(screen: Screen)
    fun home()
    fun nextLanguage()
    fun nextEngine()
    fun resume()
    fun giveUp()
    fun hearAgain()
}

// The options of a screen, in order. engineLabel is asked for the name of the speech engine in use, by the Settings screen only.
internal fun optionsFor(screen: Screen, ui: UiStrings, engineLabel: () -> String, c: Choices): List<Option> {
    val back = Option(ui.backToMenu, c::home)
    return when (screen) {
        Screen.Home -> ui.levelNames.mapIndexed { i, name -> Option(name) { c.startLevel(i + 1) } } + listOf(
            Option(ui.learnSounds) { c.open(Screen.Learn) },
            Option(ui.howToPlay) { c.open(Screen.Help) },
            Option(ui.settings) { c.open(Screen.Settings) },
        )
        Screen.Settings -> listOf(Option(ui.language, c::nextLanguage), Option(ui.voiceEngine(engineLabel()), c::nextEngine), back)
        is Screen.Paused -> listOf(Option(ui.resume, c::resume), Option(ui.giveUp, c::giveUp), back)
        is Screen.Summary -> listOf(Option(ui.hearAgain, c::hearAgain), back)
        Screen.Help, Screen.Learn, is Screen.HandOver -> listOf(back)
        Screen.NoEngine, is Screen.Play -> emptyList()
    }
}

// The text a screen shows above its options.
internal fun bodyFor(screen: Screen, ui: UiStrings): List<String> = when (screen) {
    Screen.Help -> ui.howToPlayLines
    is Screen.HandOver -> listOf(ui.handOver)
    Screen.NoEngine -> listOf(ui.noEngine)
    is Screen.Summary -> listOf(screen.result.summary)
    else -> emptyList()
}

// What the game says on entering a screen, or when the screen reader goes off on it. (The summary was spoken by the game.)
internal fun entryLines(screen: Screen, ui: UiStrings): List<Line> = when (screen) {
    Screen.Home -> listOf(menuLine(ui.greeting))
    is Screen.Paused -> listOf(menuLine(ui.paused), menuLine(ui.resume)) // and the option the cursor starts on
    Screen.Help -> ui.howToPlayLines.map { Line(it, Priority.HIGH, key = "help", many = true, waitMs = HELP_WAIT_MS) }
    else -> emptyList()
}

internal fun menuLine(text: String) = Line(text, Priority.HIGH, waitMs = MENU_WAIT_MS)

// An option the player's finger came to: it cuts off what was being said, as TalkBack does.
internal fun optionLine(text: String) = Line(text, Priority.HIGH, cut = true, key = "menu", waitMs = MENU_WAIT_MS)
