package dk.cocode.soccer.game

import dk.cocode.soccer.core.game.Records
import dk.cocode.soccer.platform.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// The player's records: read from the phone when the session starts, and written back after a level that changed them. The
// records in memory change at once, so the menu has them even when the write fails. save is Talk.save, which logs a failed
// write and lets the game go on.
internal class RecordBook(
    private val prefs: Prefs,
    private val scope: CoroutineScope,
    private val save: suspend (suspend () -> Unit) -> Unit,
) {
    var records = Records()
        private set

    // Reads them, then calls loaded, so the menu can show them.
    fun load(loaded: () -> Unit) {
        scope.launch {
            records = prefs.records.first()
            loaded()
        }
    }

    fun keep(new: Records) {
        if (new == records) return
        records = new
        scope.launch { save { prefs.setRecords(new) } }
    }
}
