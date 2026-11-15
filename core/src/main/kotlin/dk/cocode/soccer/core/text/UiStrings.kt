package dk.cocode.soccer.core.text

// The lines of the menus, settings, hand-over and pause screens, and how to play: what the app takes of the trial's
// en-phone.js (drag steering only), plus the lines only the app has. Tilt, the keyboard, the page's own chrome, the
// voice check and the results table have no strings here. The words of the game itself are in GameStrings.
interface UiStrings {
    val greeting: String // spoken on Home when the screen reader is off
    val levelNames: List<String> // the three level items, in order
    val learnSounds: String
    val howToPlay: String // the menu item
    val howToPlayLines: List<String> // what it says: the phone gestures
    val settings: String
    val language: String // the Settings item, with the current language: "Language: English"
    fun voiceEngine(name: String): String // the Settings item, with the current speech engine
    val resume: String
    val paused: String // said when the pause menu opens
    val giveUp: String
    val backToMenu: String
    val hearAgain: String
    val handOver: String // shown while the screen reader is still on: turn it off
    val dragIntro: List<String> // spoken before the first round: run, stop, kick
    val noEngine: String
    val englishOnly: String
}
