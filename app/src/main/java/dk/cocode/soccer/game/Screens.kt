package dk.cocode.soccer.game

import dk.cocode.soccer.core.game.LevelResult

// Where the player is. Play, Paused and HandOver name the level; Summary holds what the level came to.
sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data object Help : Screen
    data object Learn : Screen
    data object NoEngine : Screen // no speech engine, or it will not start: nothing the game says can be heard
    data class HandOver(val level: Int) : Screen // waiting for the screen reader to go off
    data class Play(val level: Int) : Screen
    data class Paused(val level: Int) : Screen
    data class Summary(val result: LevelResult) : Screen
}

// The form a screen change takes in the SoccerEvent log, which the end-to-end script reads: "Play(level=2)".
internal val Screen.logName: String
    get() = if (this is Screen.Summary) "Summary(level=${result.level})" else toString()

// Play and Learn make sound: the audio output runs on them and on nothing else.
internal val Screen.makesSound: Boolean get() = this is Screen.Play || this === Screen.Learn
