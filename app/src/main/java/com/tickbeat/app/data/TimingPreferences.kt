package com.tickbeat.app.data

import android.content.Context

/**
 * Persisted timing compensation for follow / imitate / sight-read judging.
 *
 * [inputOffsetMs]: median systematic late bias of taps vs audible click.
 * Positive means taps land late (e.g. extra audio path delay); judges use `tap - offset`.
 */
class TimingPreferences private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var inputOffsetMs: Int
        get() = prefs.getInt(KEY_INPUT_OFFSET, 0).coerceIn(MIN_OFFSET, MAX_OFFSET)
        set(value) {
            prefs.edit().putInt(KEY_INPUT_OFFSET, value.coerceIn(MIN_OFFSET, MAX_OFFSET)).apply()
        }

    companion object {
        const val MIN_OFFSET = -150
        const val MAX_OFFSET = 150

        private const val PREFS = "tickbeat_timing"
        private const val KEY_INPUT_OFFSET = "input_offset_ms"

        @Volatile
        private var instance: TimingPreferences? = null

        fun get(context: Context): TimingPreferences {
            return instance ?: synchronized(this) {
                instance ?: TimingPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
