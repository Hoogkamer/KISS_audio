package com.michael.kissaudio

import android.app.Application
import com.michael.kissaudio.data.AppDatabase

class KISSAudioApp : Application() {
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }
}
