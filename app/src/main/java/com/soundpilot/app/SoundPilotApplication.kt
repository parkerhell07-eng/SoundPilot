package com.soundpilot.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.soundpilot.app.di.ServiceLocator

class SoundPilotApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            PLAYBACK_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "playback"
    }
}
