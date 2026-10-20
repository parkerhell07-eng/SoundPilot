package com.soundpilot.app.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import com.soundpilot.app.data.EqualizerSettings
import com.soundpilot.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Virtualizer در API 36 منسوخ اعلام شده اما هنوز کاملاً کار می‌کند؛
// استفاده اختیاری و ایمن است و نبودش هیچ‌گاه باعث کرش نمی‌شود.

/**
 * کنترل‌کننده افکت‌های صوتی واقعی اندروید، متصل به نشست صوتی ExoPlayer.
 *
 * - قابلیت‌های دستگاه (تعداد باند، بازه فرکانسی، BassBoost/Virtualizer) در زمان
 *   اتصال تشخیص داده می‌شود؛ نبود هیچ‌یک باعث کرش نمی‌شود.
 * - تنظیمات از DataStore بازیابی و با باندهای واقعی دستگاه تطبیق داده می‌شود.
 * - منابع صوتی هنگام تغییر نشست یا نابودی سرویس به‌درستی آزاد می‌شوند.
 */
@Suppress("DEPRECATION")
class AudioEffectsController(
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = 0
    private var capabilities: EqualizerCapabilities = EqualizerCapabilities.UNSUPPORTED

    val isAttached: Boolean get() = equalizer != null

    /** اتصال/اتصال مجدد افکت‌ها به شناسه نشست صوتی پلیر */
    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && equalizer != null) return
        release()
        currentSessionId = audioSessionId

        var detected = EqualizerCapabilities.UNSUPPORTED
        try {
            val eq = Equalizer(EFFECT_PRIORITY, audioSessionId)
            equalizer = eq
            val bandCount = eq.numberOfBands.toInt()
            val levelRange = eq.bandLevelRange // [min, max] بر حسب میلی‌بِل
            val centers = (0 until bandCount).map { eq.getCenterFreq(it.toShort()) / 1000 }
            detected = EqualizerCapabilities(
                supported = bandCount > 0,
                bandCount = bandCount,
                centersHz = centers,
                minDb = levelRange[0] / 100f,
                maxDb = levelRange[1] / 100f,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Equalizer روی این نشست صوتی در دسترس نیست", e)
            runCatching { equalizer?.release() }
            equalizer = null
        }

        try {
            bassBoost = BassBoost(EFFECT_PRIORITY, audioSessionId)
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost در دسترس نیست", e)
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(EFFECT_PRIORITY, audioSessionId)
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer در دسترس نیست", e)
            virtualizer = null
        }

        capabilities = if (equalizer == null) {
            EqualizerCapabilities.UNSUPPORTED
        } else {
            detected.copy(
                bassSupported = bassBoost != null,
                virtualizerSupported = virtualizer != null,
            )
        }
        AudioEffectsHub.publish(capabilities, sessionActive = true)

        // بازیابی تنظیمات ذخیره‌شده و تطبیق با باندهای واقعی دستگاه
        scope.launch {
            val saved = settingsRepository.equalizerSettings.first()
            val state = adapt(saved)
            AudioEffectsHub.setDesiredState(state)
            if (state.gainsDb != saved.gainsDb || capabilities.bandCount != saved.bandCount) {
                settingsRepository.saveEqualizer(
                    EqualizerSettings(
                        enabled = state.enabled,
                        profileId = state.profileId,
                        gainsDb = state.gainsDb,
                        bassStrength = state.bassStrength,
                        virtualizerStrength = state.virtualizerStrength,
                        bandCount = capabilities.bandCount,
                    ),
                )
            }
        }
    }

    private fun adapt(saved: EqualizerSettings): EqState {
        if (!capabilities.supported) {
            return EqState(
                enabled = saved.enabled,
                profileId = saved.profileId,
                gainsDb = emptyList(),
                bassStrength = saved.bassStrength,
                virtualizerStrength = saved.virtualizerStrength,
            )
        }
        val gains = if (saved.gainsDb.size == capabilities.bandCount) {
            saved.gainsDb.map { it.coerceIn(capabilities.minDb, capabilities.maxDb) }
        } else {
            // تعداد باندها تغییر کرده یا بار اول است → محاسبه از منحنی پروفایل
            EqualizerMath.gainsFor(
                EqualizerPresets.byId(saved.profileId),
                capabilities.centersHz,
                capabilities.minDb,
                capabilities.maxDb,
            )
        }
        return EqState(
            enabled = saved.enabled,
            profileId = saved.profileId,
            gainsDb = gains,
            bassStrength = saved.bassStrength,
            virtualizerStrength = saved.virtualizerStrength,
        )
    }

    /** اعمال همزمان وضعیت روی همه افکت‌ها؛ خطاهای هر افکت مستقل مدیریت می‌شود */
    fun apply(state: EqState) {
        val eq = equalizer ?: return
        try {
            val bandCount = eq.numberOfBands.toInt()
            for (i in 0 until minOf(bandCount, state.gainsDb.size)) {
                val mb = EqualizerMath.dbToMillibels(
                    state.gainsDb[i],
                    capabilities.minDb,
                    capabilities.maxDb,
                )
                eq.setBandLevel(i.toShort(), mb.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "اعمال سطوح اکولایزر ناموفق بود", e)
        }
        try {
            bassBoost?.setStrength(state.bassStrength.coerceIn(0, 1000).toShort())
        } catch (e: Exception) {
            Log.w(TAG, "setStrength برای BassBoost ناموفق بود", e)
        }
        try {
            virtualizer?.setStrength(state.virtualizerStrength.coerceIn(0, 1000).toShort())
        } catch (e: Exception) {
            Log.w(TAG, "setStrength برای Virtualizer ناموفق بود", e)
        }
        try {
            eq.enabled = state.enabled
            bassBoost?.enabled = state.enabled
            virtualizer?.enabled = state.enabled
        } catch (e: Exception) {
            Log.w(TAG, "تغییر وضعیت فعال افکت‌ها ناموفق بود", e)
        }
    }

    /** آزادسازی کامل منابع صوتی — بدون مختل‌کردن خود پخش */
    fun release() {
        runCatching { equalizer?.enabled = false }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentSessionId = 0
        capabilities = EqualizerCapabilities.UNSUPPORTED
        AudioEffectsHub.publish(EqualizerCapabilities.UNSUPPORTED, sessionActive = false)
    }

    companion object {
        private const val TAG = "AudioEffects"
        private const val EFFECT_PRIORITY = 0
    }
}
