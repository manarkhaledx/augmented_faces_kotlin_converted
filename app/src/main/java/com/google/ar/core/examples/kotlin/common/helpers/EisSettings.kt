package com.google.ar.core.examples.kotlin.common.helpers

import android.content.Context
import android.content.SharedPreferences

/**
 * A class providing persistent EIS preference across instances using `android.content.SharedPreferences`.
 */
class EisSettings {
    private var eisEnabled = false
    private var sharedPreferences: SharedPreferences? = null

    /** Creates shared preference entry for EIS setting.  */
    fun onCreate(context: Context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_ID, Context.MODE_PRIVATE)
        eisEnabled = sharedPreferences?.getBoolean(SHARED_PREFERENCE_EIS_ENABLED, false) ?: false
    }

    /** Returns saved EIS state.  */
    fun isEisEnabled(): Boolean {
        return eisEnabled
    }

    /** Sets and saves the EIS using `android.content.SharedPreferences`. */
    fun setEisEnabled(enable: Boolean) {
        if (enable == eisEnabled) {
            return
        }

        eisEnabled = enable
        sharedPreferences?.let { prefs ->
            prefs.edit().putBoolean(SHARED_PREFERENCE_EIS_ENABLED, eisEnabled).apply()
        }
    }

    companion object {
        const val SHARED_PREFERENCE_ID: String = "SHARED_PREFERENCE_EIS_OPTIONS"
        const val SHARED_PREFERENCE_EIS_ENABLED: String = "eis_enabled"
    }
}
