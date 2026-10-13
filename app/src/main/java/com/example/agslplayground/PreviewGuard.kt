package com.example.agslplayground

import android.content.Context

/**
 * Remembers which sample was starting its preview when the app died, so the next launch
 * can skip that preview instead of crashing again.
 */
object PreviewGuard {
    private const val PREFS = "preview_guard"
    private const val KEY_PENDING = "pending"
    private const val KEY_BLOCKED = "blocked"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Call at process start: a sample still marked pending means the app died while previewing it. */
    fun blockPendingFromLastRun(context: Context) {
        val pending = prefs(context).getString(KEY_PENDING, null) ?: return
        prefs(context).edit()
            .putStringSet(KEY_BLOCKED, blocked(context) + pending)
            .remove(KEY_PENDING)
            .commit()
    }

    fun blocked(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED, emptySet())?.toSet() ?: emptySet()

    // commit() is synchronous on purpose: the process may die right after this call.
    fun markPending(context: Context, id: String) {
        prefs(context).edit().putString(KEY_PENDING, id).commit()
    }

    fun clearPending(context: Context) {
        prefs(context).edit().remove(KEY_PENDING).commit()
    }

    fun unblock(context: Context, id: String) {
        prefs(context).edit().putStringSet(KEY_BLOCKED, blocked(context) - id).commit()
    }

    fun unblockAll(context: Context) {
        prefs(context).edit().remove(KEY_BLOCKED).commit()
    }
}
