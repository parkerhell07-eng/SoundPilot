package com.soundpilot.app.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.media3.common.AuxEffectInfo
import com.soundpilot.app.data.EqualizerSettings
import com.soundpilot.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Virtualizer در API 36 منسوخ اعلام شده اما هنوز کاملاً کار می‌کند؛
// استفاده اختیاری و ایمن است و نبودش هیچ‌گاه باعث کرش نمی‌شود.

/**
 * موتور حرفه‌ای افکت‌های صوتی واقعی اندروید، متصل به نشست صوتی پایدار برنامه.
 *
 * تفاوت با نسخه‌های قبل (ریشه رفع باگ «سازگار نیست» روی دستگاه واقعی):
 * - نشست صوتی توسط خود برنامه ساخته و روی پلیر ست می‌شود (توسط PlaybackService)
 *   و با تغییر خروجی (بلوتوث/هدفون) عوض نمی‌شود؛ افکت‌ها وصل می‌مانند.
 * - اگر ایجاد افکت در لحظه‌ای شکست بخورد (مثلاً بلافاصله بعد از وصل‌شدن ضبط
 *   خودرو که سیستم صوتی سامسونگ هنوز مسیر را جا نینداخته)، موتور با
 *   عقب‌نشینی زمانی (۰٫۵ تا ۸ ثانیه) تا ۵ بار خودش دوباره تلاش می‌کند.
 * - با شروع پخش و با تغییر خروجی صوتی هم دوباره تلاش می‌شود؛ پس دیگر هیچ‌گاه
 *   وضعیت «سازگار نیست» دائمی به کاربر نشان داده نمی‌شود.
 * - وضعیت دقیق موتور (Preparing/Ready/Unavailable) برای UI منتشر می‌شود تا
 *   پیام‌ها صادقانه و همراه دکمه «تلاش مجدد» باشند.
 *
 * - قابلیت‌های دستگاه (تعداد باند، بازه فرکانسی، BassBoost/Virtualizer/Loudness/Reverb)
 *   در زمان اتصال تشخیص داده می‌شود؛ نبود هیچ‌یک باعث کرش نمی‌شود.
 * - تنظیمات از DataStore بازیابی و با باندهای واقعی دستگاه تطبیق داده می‌شود.
 * - «حافظه خروجی»: با تغییر خروجی در صورت فعال‌بودن تعویض خودکار، آخرین پریست
 *   ذخیره‌شده همان خروجی اعمال می‌شود.
 * - طنین (Reverb) به‌صورت افکت کمکی به AudioTrack پلیر وصل می‌شود.
 */
@Suppress("DEPRECATION")
class AudioEffectsController(
    context: Context,
    private val scope: CoroutineScope,
    private val settingsRepository: SettingsRepository,
) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var reverb: PresetReverb? = null
    private var currentSessionId: Int = 0
    private var capabilities: EqualizerCapabilities = EqualizerCapabilities.UNSUPPORTED

    /** شماره تلاش جاری در توالی اتصال (از ۱) */
    private var attempt = 1

    /** آخرین شکست ثبت‌شده — برای پیام دقیق کاربر */
    private var lastFailure: EffectFailure = EffectFailure.GENERIC

    /** توالی اتصال جاری؛ با شروع توالی جدید لغو می‌شود */
    private var engineJob: Job? = null

    /**
     * مسیر اتصال افکت کمکی (طنین) به AudioTrack پلیر.
     * توسط PlaybackService با setAuxEffectInfo/clearAuxEffectInfo پلیر سیم‌کشی می‌شود.
     */
    @Volatile
    var auxEffectSink: ((AuxEffectInfo?) -> Unit)? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var currentCategory: AudioDeviceCategory = AudioDeviceCategory.SPEAKER

    val isAttached: Boolean get() = equalizer != null

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            onOutputDevicesChanged()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            onOutputDevicesChanged()
        }
    }

    init {
        currentCategory = detectCategory()
        AudioEffectsHub.publishDeviceCategory(currentCategory)
        runCatching {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
        }.onFailure { Log.w(TAG, "ثبت AudioDeviceCallback ناموفق بود", it) }
    }

    // ───────────────────────── توالی اتصال موتور ─────────────────────────

    /** اتصال/اتصال مجدد افکت‌ها به شناسه نشست صوتی پلیر */
    fun attach(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        if (audioSessionId == currentSessionId && capabilities.supported) return
        beginAttachSequence(audioSessionId)
    }

    /** تلاش مجدد با فرمان کاربر (دکمه «تلاش مجدد» در UI) */
    fun retryFromUser() {
        val sessionId = currentSessionId
        if (sessionId <= 0) return
        beginAttachSequence(sessionId)
    }

    /** با شروع واقعی پخش اگر افکت‌ها هنوز وصل نیستند، دوباره تلاش کن */
    fun onPlaybackStarted() {
        if (capabilities.supported) return
        val sessionId = currentSessionId
        if (sessionId <= 0) return
        scope.launch {
            // فرصت کوتاه برای جاافتادن مسیر صوتی روی دستگاه‌های کندتر
            delay(EffectRetryPolicy.SETTLE_DELAY_MS)
            beginAttachSequence(sessionId)
        }
    }

    /** شروع یک توالی کامل اتصال: تلاش اول + تلاش‌های مجدد خودکار با backoff */
    private fun beginAttachSequence(sessionId: Int) {
        engineJob?.cancel()
        engineJob = scope.launch {
            runAttachSequence(sessionId)
        }
    }

    private suspend fun runAttachSequence(sessionId: Int) {
        currentSessionId = sessionId
        attempt = 1
        AudioEffectsHub.publishStatus(EffectsStatus.Preparing(attempt))
        if (tryAttachEffects()) {
            onEffectsReady()
            return
        }
        // تلاش‌های مجدد خودکار — راه‌حل ریشه‌ای «سازگار نیست» موقت
        while (EffectRetryPolicy.shouldRetry(attempt)) {
            delay(EffectRetryPolicy.delayBeforeAttempt(attempt))
            attempt++
            Log.i(TAG, "تلاش مجدد اتصال افکت‌ها (تلاش $attempt از ${EffectRetryPolicy.MAX_ATTEMPTS})")
            AudioEffectsHub.publishStatus(EffectsStatus.Preparing(attempt))
            if (tryAttachEffects()) {
                onEffectsReady()
                return
            }
        }
        Log.w(TAG, "اتصال افکت‌ها پس از ${EffectRetryPolicy.MAX_ATTEMPTS} تلاش ناموفق ماند")
        AudioEffectsHub.publishStatus(
            EffectsStatus.Unavailable(EffectFailureClassifier.userMessage(lastFailure)),
        )
    }

    /** یک تلاش کامل برای ساختن همه افکت‌ها روی نشست جاری — true یعنی موفق */
    private fun tryAttachEffects(): Boolean {
        val sessionId = currentSessionId
        if (sessionId <= 0) return false
        releaseEffectsQuiet()

        var detected = EqualizerCapabilities.UNSUPPORTED
        try {
            val eq = Equalizer(EFFECT_PRIORITY, sessionId)
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
            lastFailure = EffectFailureClassifier.classify(e)
            Log.w(TAG, "Equalizer روی این نشست صوتی هنوز در دسترس نیست: ${e.message}")
            runCatching { equalizer?.release() }
            equalizer = null
        }

        if (equalizer == null) {
            // بدون اکولایزر، بقیه افکت‌ها بی‌معنا هستند؛ منابع جزئی را آزاد کن
            releaseEffectsQuiet()
            return false
        }

        try {
            bassBoost = BassBoost(EFFECT_PRIORITY, sessionId)
        } catch (e: Exception) {
            Log.w(TAG, "BassBoost در دسترس نیست", e)
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(EFFECT_PRIORITY, sessionId)
        } catch (e: Exception) {
            Log.w(TAG, "Virtualizer در دسترس نیست", e)
            virtualizer = null
        }

        try {
            loudness = LoudnessEnhancer(sessionId)
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer در دسترس نیست", e)
            loudness = null
        }

        try {
            reverb = PresetReverb(EFFECT_PRIORITY, sessionId)
        } catch (e: Exception) {
            Log.w(TAG, "PresetReverb در دسترس نیست", e)
            runCatching { reverb?.release() }
            reverb = null
        }

        capabilities = detected.copy(
            bassSupported = bassBoost != null,
            virtualizerSupported = virtualizer != null,
            loudnessSupported = loudness != null,
            reverbSupported = reverb != null && auxEffectSink != null,
        )
        return true
    }

    /** اتصال موفق → انتشار وضعیت + بازیابی تنظیمات ذخیره‌شده */
    private fun onEffectsReady() {
        AudioEffectsHub.publishStatus(EffectsStatus.Ready(capabilities))
        // بازیابی تنظیمات ذخیره‌شده؛ اگر برای خروجی فعلی پروفایل مستقل ذخیره شده باشد
        // همان پروفایل مقدم است (قابلیت حافظه خروجی)
        scope.launch {
            val saved = settingsRepository.equalizerSettings.first()
            val autoSwitch = settingsRepository.eqAutoSwitch.first()
            val devicePresetId = if (autoSwitch) {
                settingsRepository.deviceProfiles.first()[currentCategory.name]
            } else {
                null
            }
            val state = if (devicePresetId != null) {
                stateFromPreset(SoundPresets.byId(devicePresetId), enabled = true)
            } else {
                adapt(saved)
            }
            AudioEffectsHub.setDesiredState(state)
            persistIfNeeded(state, saved)
        }
    }

    /** ساخت وضعیت کامل از یک پریست آماده، هماهنگ با باندهای واقعی دستگاه */
    private fun stateFromPreset(preset: SoundPreset, enabled: Boolean): EqState {
        val cap = capabilities
        val gains = if (cap.supported) {
            EqualizerMath.gainsFor(
                EqCurve(preset.id, preset.label, preset.curve),
                cap.centersHz,
                cap.minDb,
                cap.maxDb,
            )
        } else {
            emptyList()
        }
        return EqState(
            enabled = enabled,
            profileId = preset.id,
            gainsDb = gains,
            bassStrength = preset.bass.coerceIn(0, 1000),
            virtualizerStrength = preset.surround.coerceIn(0, 1000),
            loudnessGainMb = preset.loudnessMb.coerceIn(0, MAX_LOUDNESS_GAIN_MB),
            reverbId = preset.reverb.id,
            reverbAmount = preset.reverbAmount.coerceIn(0, 100),
        )
    }

    private suspend fun persistIfNeeded(state: EqState, saved: EqualizerSettings) {
        if (state.gainsDb != saved.gainsDb || capabilities.bandCount != saved.bandCount ||
            state.reverbId != saved.reverbId || state.reverbAmount != saved.reverbAmount
        ) {
            settingsRepository.saveEqualizer(
                EqualizerSettings(
                    enabled = state.enabled,
                    profileId = state.profileId,
                    gainsDb = state.gainsDb,
                    bassStrength = state.bassStrength,
                    virtualizerStrength = state.virtualizerStrength,
                    loudnessGainMb = state.loudnessGainMb,
                    bandCount = capabilities.bandCount,
                    reverbId = state.reverbId,
                    reverbAmount = state.reverbAmount,
                ),
            )
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
                loudnessGainMb = saved.loudnessGainMb,
                reverbId = saved.reverbId,
                reverbAmount = saved.reverbAmount,
            )
        }
        val gains = if (saved.gainsDb.size == capabilities.bandCount) {
            saved.gainsDb.map { it.coerceIn(capabilities.minDb, capabilities.maxDb) }
        } else {
            // تعداد باندها تغییر کرده یا بار اول است → محاسبه از پریست ذخیره‌شده
            stateFromPreset(SoundPresets.byId(saved.profileId), saved.enabled).gainsDb
        }
        return EqState(
            enabled = saved.enabled,
            profileId = saved.profileId,
            gainsDb = gains,
            bassStrength = saved.bassStrength,
            virtualizerStrength = saved.virtualizerStrength,
            loudnessGainMb = saved.loudnessGainMb,
            reverbId = saved.reverbId,
            reverbAmount = saved.reverbAmount,
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
            loudness?.setTargetGain(
                state.loudnessGainMb.coerceIn(0, MAX_LOUDNESS_GAIN_MB),
            )
        } catch (e: Exception) {
            Log.w(TAG, "setTargetGain برای LoudnessEnhancer ناموفق بود", e)
        }
        applyReverb(state)
        try {
            eq.enabled = state.enabled
            bassBoost?.enabled = state.enabled
            virtualizer?.enabled = state.enabled
            loudness?.enabled = state.enabled && state.loudnessGainMb > 0
        } catch (e: Exception) {
            Log.w(TAG, "تغییر وضعیت فعال افکت‌ها ناموفق بود", e)
        }
    }

    /** اعمال طنین: انتخاب preset سخت‌افزاری + ارسال صدا به آن از طریق AudioTrack */
    private fun applyReverb(state: EqState) {
        val rv = reverb ?: return
        val active = state.enabled && state.reverb != ReverbPreset.OFF && state.reverbAmount > 0
        try {
            rv.preset = state.reverb.id.toShort()
        } catch (e: Exception) {
            Log.w(TAG, "انتخاب preset طنین ناموفق بود", e)
        }
        try {
            rv.enabled = active
        } catch (e: Exception) {
            Log.w(TAG, "فعال‌سازی طنین ناموفق بود", e)
        }
        try {
            auxEffectSink?.invoke(
                if (active) {
                    AuxEffectInfo(rv.id, state.reverbAmount.coerceIn(0, 100) / 100f)
                } else {
                    null
                },
            )
        } catch (e: Exception) {
            Log.w(TAG, "اتصال طنین به AudioTrack ناموفق بود", e)
        }
    }

    /** آزادسازی کامل — بدون مختل‌کردن خود پخش */
    fun release() {
        engineJob?.cancel()
        releaseEffects()
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
    }

    private fun releaseEffects() {
        releaseEffectsQuiet()
        AudioEffectsHub.publishStatus(EffectsStatus.Idle)
    }

    private fun releaseEffectsQuiet() {
        runCatching { equalizer?.enabled = false }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudness?.release() }
        runCatching { reverb?.enabled = false }
        runCatching { reverb?.release() }
        runCatching { auxEffectSink?.invoke(null) }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        reverb = null
        capabilities = EqualizerCapabilities.UNSUPPORTED
    }

    // ───────────────────────── حافظه خروجی ─────────────────────────

    /** تشخیص رده خروجی فعال با اولویت: بلوتوث > USB > سیمی > HDMI > بلندگو */
    private fun detectCategory(): AudioDeviceCategory {
        val categories = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .map { AudioDeviceCategory.fromDeviceType(it.type) }
                .toSet()
        }.getOrDefault(emptySet())
        return when {
            AudioDeviceCategory.BLUETOOTH in categories -> AudioDeviceCategory.BLUETOOTH
            AudioDeviceCategory.USB in categories -> AudioDeviceCategory.USB
            AudioDeviceCategory.WIRED in categories -> AudioDeviceCategory.WIRED
            AudioDeviceCategory.HDMI in categories -> AudioDeviceCategory.HDMI
            else -> AudioDeviceCategory.SPEAKER
        }
    }

    private fun onOutputDevicesChanged() {
        val newCategory = detectCategory()
        if (newCategory == currentCategory) return
        currentCategory = newCategory
        AudioEffectsHub.publishDeviceCategory(newCategory)

        if (!capabilities.supported) {
            // خروجی عوض شد — سیستم صوتی برخی دستگاه‌ها افکت را فقط بعد از
            // جاافتادن مسیر جدید می‌پذیرد؛ یک تلاش کامل جدید شروع کن
            val sessionId = currentSessionId
            if (sessionId > 0) {
                scope.launch {
                    delay(EffectRetryPolicy.SETTLE_DELAY_MS)
                    beginAttachSequence(sessionId)
                }
            }
            return
        }

        scope.launch {
            val autoSwitch = settingsRepository.eqAutoSwitch.first()
            if (!autoSwitch) return@launch
            val presetId = settingsRepository.deviceProfiles.first()[newCategory.name] ?: return@launch
            // تعویض خودکار پریست ذخیره‌شده این خروجی — صدای مناسب همان دستگاه
            val state = stateFromPreset(SoundPresets.byId(presetId), enabled = true)
            AudioEffectsHub.setDesiredState(state)
            settingsRepository.saveEqualizer(
                EqualizerSettings(
                    enabled = state.enabled,
                    profileId = state.profileId,
                    gainsDb = state.gainsDb,
                    bassStrength = state.bassStrength,
                    virtualizerStrength = state.virtualizerStrength,
                    loudnessGainMb = state.loudnessGainMb,
                    bandCount = capabilities.bandCount,
                    reverbId = state.reverbId,
                    reverbAmount = state.reverbAmount,
                ),
            )
        }
    }

    companion object {
        private const val TAG = "AudioEffects"
        private const val EFFECT_PRIORITY = 0

        /** سقف امن تقویت بلندی: ۱۵۰۰ میلی‌بِل = ۱۵ دسی‌بل */
        const val MAX_LOUDNESS_GAIN_MB = 1500
    }
}
