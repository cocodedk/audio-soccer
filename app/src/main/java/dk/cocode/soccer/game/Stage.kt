package dk.cocode.soccer.game

import dk.cocode.soccer.core.input.Menu
import dk.cocode.soccer.core.input.MenuInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The screen that is up, its options and the cursor over them: what the menus show, and what a touch or a button on them
// does. Entering a screen is reported to onChange(before, after) once the screen is set, before its options are read.
// ready: an engine speaks. Until then the options show but nothing is chosen.
internal class Stage(
    private val talk: Talk,
    private val choices: Choices,
    private val log: (String) -> Unit,
    private val onChange: (Screen, Screen) -> Unit,
) {
    var ready = false
    private var menu: Menu? = null
    var options = emptyList<Option>()
        private set

    private val mutableScreen = MutableStateFlow<Screen>(Screen.Home)
    private val mutableItems = MutableStateFlow<List<String>>(emptyList())
    private val mutableBody = MutableStateFlow<List<String>>(emptyList())
    private val mutableFocus = MutableStateFlow(0)

    val screen: StateFlow<Screen> = mutableScreen.asStateFlow()
    val items: StateFlow<List<String>> = mutableItems.asStateFlow() // the option texts
    val body: StateFlow<List<String>> = mutableBody.asStateFlow() // the text shown above them
    val focus: StateFlow<Int> = mutableFocus.asStateFlow()
    val current: Screen get() = mutableScreen.value

    fun show(s: Screen) {
        val before = current
        mutableScreen.value = s
        if (s == Screen.NoEngine) ready = false
        log("screen ${s.logName}")
        onChange(before, s)
        menu = null
        refresh()
    }

    // The options and the text of the screen, again: the language or the engine changed, or the screen did. The cursor stays.
    fun refresh() {
        val s = current
        options = optionsFor(s, talk.ui, { talk.engineLabel }, choices)
        mutableItems.value = options.map { it.text }
        mutableBody.value = bodyFor(s, talk.ui)
        val index = menu?.index ?: 0
        menu = if (options.isEmpty()) null else Menu(options.size).also { it.focus(index) }
        mutableFocus.value = index
    }

    fun menu(input: MenuInput) {
        val m = menu ?: return
        if (!ready) return
        when (input) {
            is MenuInput.Focus -> m.focus(input.index)
            MenuInput.Next -> m.next()
            MenuInput.Prev -> m.prev()
            MenuInput.Activate -> return choose(m.index)
        }
        mutableFocus.value = m.index
        sayOption(m.index)
    }

    // Chooses an option, after moving the cursor to it: from a touch it is the option the cursor is on, from the screen reader
    // player's double-tap on a button any.
    fun choose(index: Int) {
        val option = options.getOrNull(index) ?: return
        if (!ready) return
        menu?.focus(index)
        mutableFocus.value = index
        option.choose()
    }

    // What the game says on entering the screen, or when the screen reader goes off on it.
    fun announce() {
        if (ready) entryLines(current, talk.ui).forEach(talk::say)
    }

    fun sayOption(i: Int) = talk.say(optionLine(options[i].text))

    // An engine speaks: the menus work, in the language it speaks, and say what the player needs to hear.
    fun engineUp() {
        ready = true
        if (current == Screen.NoEngine) show(Screen.Home) else refresh()
        sayEnglishOnly()
        announce()
    }

    // A setting was stepped: its option, now with the new value and in the new language, then the notice if Persian was not had.
    fun saySetting() {
        refresh()
        sayOption(focus.value)
        sayEnglishOnly()
    }

    private fun sayEnglishOnly() {
        if (talk.fellBack) talk.say(menuLine(talk.ui.englishOnly))
    }
}
