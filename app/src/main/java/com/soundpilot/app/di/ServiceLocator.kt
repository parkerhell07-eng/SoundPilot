package com.soundpilot.app.di

import android.content.Context
import com.soundpilot.app.data.MediaStoreMusicRepository
import com.soundpilot.app.data.SettingsRepository
import com.soundpilot.app.update.UpdateManager

/**
 * تزریق وابستگی سبک و بدون فریم‌ورک.
 * پروژه کوچک و تک‌پروسه است؛ Hilt برای این مقیاس ضروری نبود.
 */
object ServiceLocator {

    lateinit var appContext: Context
        private set

    val musicRepository: MediaStoreMusicRepository by lazy { MediaStoreMusicRepository(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val updateManager: UpdateManager by lazy { UpdateManager(appContext) }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
