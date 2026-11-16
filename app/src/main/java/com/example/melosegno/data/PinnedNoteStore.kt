package com.example.melosegno.data

import android.content.Context

/**
 * Id della nota fissata nel widget, in SharedPreferences dedicati.
 * 0 significa "nessuna nota fissata".
 */
class PinnedNoteStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun pinnedNoteId(): Long = prefs.getLong(KEY_PINNED_ID, 0L)

    fun pin(id: Long) {
        prefs.edit().putLong(KEY_PINNED_ID, id).commit()
    }

    fun unpin() {
        prefs.edit().remove(KEY_PINNED_ID).commit()
    }

    private companion object {
        const val PREFS_NAME = "MeLoSegno_widget"
        const val KEY_PINNED_ID = "pinned_note_id"
    }
}
