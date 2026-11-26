package com.rahmatulula.motocare

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.rahmatulula.motocare.util.NotificationHelper
import com.rahmatulula.motocare.util.Prefs
import com.rahmatulula.motocare.worker.ReminderWorker

class MotoCareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Prefs.themeMode(this))
        NotificationHelper.createChannel(this)
        ReminderWorker.schedule(this)
    }
}
