package dk.cocode.soccer.core.text

// English: the lines of the menus, settings, hand-over and pause screens, and how to play. From prototype/js/i18n/
// en-phone.js where it has them, trimmed as the owner approved on 2026-11-13; the rest is new.
internal object EnUi : UiStrings {
    override val greeting = "Audio Soccer. Touch the screen to hear the options. Double-tap to choose."
    override val levelNames = listOf(
        "Find the ball",
        "Score a goal",
        "Beat the keeper",
    )
    override val learnSounds = "Learn the sounds"
    override val howToPlay = "How to play"
    override val howToPlayLines = listOf(
        "Put a finger anywhere on the screen and drag it the way you want to run.",
        "Up the screen is toward the goal. Lift your finger to stop.",
        "Tap to kick, with any finger, even while you steer.",
        "Tap with two fingers to hear where you are.",
        "Tap with three fingers to pause.",
    )
    override val settings = "Settings"
    override val language = "Language: English"
    override fun voiceEngine(name: String) = "Voice: $name"
    override val resume = "Resume"
    override val paused = "Paused"
    override val giveUp = "Give up this round"
    override val backToMenu = "Back to menu"
    override val hearAgain = "Hear again"
    override val handOver = "Turn your screen reader off now."
    override val dragIntro = listOf(
        "Drag a finger on the screen to run. Up the screen is toward the goal. Lift your finger to stop.",
        "Tap to kick, with any finger, even while you steer.",
    )
    override val noEngine = "This phone has no speech engine, so the game cannot speak. Install one, for example eSpeak, and open the game again."
    override val englishOnly = "This voice cannot speak Persian, so the game speaks English."
}
