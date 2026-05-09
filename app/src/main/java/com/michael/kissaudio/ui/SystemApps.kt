package com.michael.kissaudio.ui

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

object SystemApps {
    fun launchClock(context: Context) {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If the standard intent fails, there isn't much we can do without QUERY_ALL_PACKAGES
            // but we can try to open the system settings as a last resort
            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
            settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(settingsIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
