package dk.cocode.soccer.core.text

// English: the lines of the menus, settings, hand-over and pause screens, and how to play. From prototype/js/i18n/
// en-phone.js where it has them, trimmed as the owner approved on 2026-11-13; the rest is new. The kicking tip is
// the last line of how to play, not part of the intro.
internal object EnUi : UiStrings {
    override val greeting = "Audio Soccer. Hold a finger on the screen to hear the options, or swipe to move through them. Tap to hear the option again, double-tap to choose."
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
        "A kick goes straight up the pitch. Line up with the goal, within about twenty metres, and kick.",
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
        "Put a finger anywhere on the screen and drag it the way you want to run. Up the screen is toward the goal. Lift your finger to stop.",
        "Tap to kick, with any finger, even while you steer.",
    )
    override val noEngine = "This phone has no speech engine, so the game cannot speak. Install one, for example eSpeak. After installing it, open the speech engine's own app once, so it can get its voices ready. Then open the game again."
    override val englishOnly = "This voice cannot speak Persian, so the game speaks English."
}
