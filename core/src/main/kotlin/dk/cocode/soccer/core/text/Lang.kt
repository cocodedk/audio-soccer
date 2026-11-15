package dk.cocode.soccer.core.text

// The languages the game speaks. Every word comes from GameStrings and UiStrings of the chosen one.
enum class Lang { EN, FA }

fun gameStrings(lang: Lang): GameStrings = when (lang) {
    Lang.EN -> EnGame
    Lang.FA -> FaGame
}

fun uiStrings(lang: Lang): UiStrings = when (lang) {
    Lang.EN -> EnUi
    Lang.FA -> FaUi
}
