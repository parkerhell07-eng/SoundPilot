package com.soundpilot.app.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundpilot.app.audio.AudioDeviceCategory
import com.soundpilot.app.audio.AudioEffectsController
import com.soundpilot.app.audio.AudioEffectsHub
import com.soundpilot.app.audio.EqState
import com.soundpilot.app.audio.EqualizerMath
import com.soundpilot.app.audio.EqualizerPresets
import com.soundpilot.app.audio.ReverbPreset
import com.soundpilot.app.audio.SoundPreset
import com.soundpilot.app.audio.SoundPresets
import com.soundpilot.app.data.EqualizerSettings
import com.soundpilot.app.di.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class EqualizerUiState(
    val supported: Boolean = false,
    val sessionActive: Boolean = false,
    val bandCount: Int = 0,
    val centersHz: List<Int> = emptyList(),
    val minDb: Float = -15f,
    val maxDb: Float = 15f,
    val enabled: Boolean = false,
    val profileId: String = "balanced",
    val gainsDb: List<Float> = emptyList(),
    val bassStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val bassSupported: Boolean = false,
    val virtualizerSupported: Boolean = false,
    val loudnessSupported: Boolean = false,
    val reverbSupported: Boolean = false,
    val reverbId: Int = 0,
    val reverbAmount: Int = 0,
    // حافظه خروجی
    val deviceCategory: AudioDeviceCategory = AudioDeviceCategory.SPEAKER,
    val autoSwitch: Boolean = true,
    val savedForCurrentDevice: Boolean = false,
) {
    /** پریست جاری برای نمایش در کاروسل */
    val currentPreset: SoundPreset get() = SoundPresets.byId(profileId)
}

/**
 * پل بین استودیو صدا و افکت‌های واقعی صوتی.
 * تغییرها بلافاصله روی نشست صوتی زنده اعمال و با تأخیر کوتاه در DataStore ذخیره می‌شوند.
 * انتخاب هر پریست برای خروجی جاری نیز به‌عنوان «پروفایل آن خروجی» ثبت می‌شود.
 */
class EqualizerViewModel : ViewModel() {

    private val settings = ServiceLocator.settingsRepository
    private var persistJob: Job? = null
    private var devicePersistJob: Job? = null

    val uiState: StateFlow<EqualizerUiState> = combine(
        AudioEffectsHub.capabilities,
        AudioEffectsHub.sessionActive,
        AudioEffectsHub.desiredState,
        AudioEffectsHub.deviceCategory,
        settings.eqAutoSwitch,
    ) { cap, active, desired, category, autoSwitch ->
        val gains = when {
            cap.supported && desired.gainsDb.size == cap.bandCount -> desired.gainsDb
            cap.supported -> EqualizerMath.gainsFor(
                SoundPresets.byId(desired.profileId).let {
                    com.soundpilot.app.audio.EqCurve(it.id, it.label, it.curve)
                },
                cap.centersHz,
                cap.minDb,
                cap.maxDb,
            )
            else -> emptyList()
        }
        val profiles = if (autoSwitch) settings.deviceProfiles.first() else emptyMap()
        EqualizerUiState(
            supported = cap.supported,
            sessionActive = active,
            bandCount = cap.bandCount,
            centersHz = cap.centersHz,
            minDb = cap.minDb,
            maxDb = cap.maxDb,
            enabled = desired.enabled,
            profileId = desired.profileId,
            gainsDb = gains,
            bassStrength = desired.bassStrength,
            virtualizerStrength = desired.virtualizerStrength,
            loudnessGainMb = desired.loudnessGainMb,
            bassSupported = cap.bassSupported,
            virtualizerSupported = cap.virtualizerSupported,
            loudnessSupported = cap.loudnessSupported,
            reverbSupported = cap.reverbSupported,
            reverbId = desired.reverbId,
            reverbAmount = desired.reverbAmount,
            deviceCategory = category,
            autoSwitch = autoSwitch,
            savedForCurrentDevice = profiles.containsKey(category.name),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EqualizerUiState())

    fun setEnabled(value: Boolean) = publishAndPersist { it.copy(enabled = value) }

    /**
     * اعمال یک پریست کامل با یک لمس.
     * انتخاب پریست یعنی کاربر صدا را می‌خواهد → افکت‌ها روشن می‌شوند.
     */
    fun applyPreset(preset: SoundPreset) {
        val cap = AudioEffectsHub.capabilities.value
        val gains = if (cap.supported) {
            EqualizerMath.gainsFor(
                com.soundpilot.app.audio.EqCurve(preset.id, preset.label, preset.curve),
                cap.centersHz,
                cap.minDb,
                cap.maxDb,
            )
        } else {
            emptyList()
        }
        publishAndPersist {
            it.copy(
                enabled = true,
                profileId = preset.id,
                gainsDb = gains,
                bassStrength = preset.bass.coerceIn(0, 1000),
                virtualizerStrength = preset.surround.coerceIn(0, 1000),
                loudnessGainMb = preset.loudnessMb.coerceIn(0, AudioEffectsController.MAX_LOUDNESS_GAIN_MB),
                reverbId = preset.reverb.id,
                reverbAmount = preset.reverbAmount.coerceIn(0, 100),
            )
        }
        // ثبت برای خروجی جاری — دفعه بعد که همین دستگاه وصل شود، همین صدا برمی‌گردد
        rememberForCurrentDevice(preset.id)
    }

    fun setBandGain(index: Int, rawDb: Float) {
        val cap = AudioEffectsHub.capabilities.value
        val range = EqualizerMath.uiRange(cap.minDb, cap.maxDb)
        // گام نیم‌دسی‌بلی برای پایداری تنظیم
        val db = ((rawDb.coerceIn(range.start, range.endInclusive)) * 2).roundToInt() / 2f
        publishAndPersist { state ->
            val gains = state.gainsDb.toMutableList().apply {
                if (index in indices) set(index, db)
            }
            state.copy(profileId = EqualizerPresets.CUSTOM_ID, gainsDb = gains)
        }
    }

    fun setBassStrength(value: Int) =
        publishAndPersist { it.copy(bassStrength = value.coerceIn(0, 1000)) }

    fun setVirtualizerStrength(value: Int) =
        publishAndPersist { it.copy(virtualizerStrength = value.coerceIn(0, 1000)) }

    /** بهره بلندی بر حسب میلی‌بِل؛ ۰ تا ۱۵۰۰ (۰ تا ۱۵ دسی‌بل) */
    fun setLoudnessGainMb(value: Int) =
        publishAndPersist { it.copy(loudnessGainMb = value.coerceIn(0, AudioEffectsController.MAX_LOUDNESS_GAIN_MB)) }

    fun setReverb(preset: ReverbPreset) =
        publishAndPersist { it.copy(reverbId = preset.id) }

    fun setReverbAmount(value: Int) =
        publishAndPersist { it.copy(reverbAmount = value.coerceIn(0, 100)) }

    fun setAutoSwitch(value: Boolean) {
        viewModelScope.launch { settings.setEqAutoSwitch(value) }
    }

    fun reset() {
        applyPresetInternal(SoundPresets.FLAT, enable = false)
    }

    private fun applyPresetInternal(preset: SoundPreset, enable: Boolean) {
        val cap = AudioEffectsHub.capabilities.value
        val gains = if (cap.supported) {
            EqualizerMath.gainsFor(
                com.soundpilot.app.audio.EqCurve(preset.id, preset.label, preset.curve),
                cap.centersHz,
                cap.minDb,
                cap.maxDb,
            )
        } else {
            emptyList()
        }
        publishAndPersist {
            it.copy(
                enabled = enable,
                profileId = preset.id,
                gainsDb = gains,
                bassStrength = preset.bass.coerceIn(0, 1000),
                virtualizerStrength = preset.surround.coerceIn(0, 1000),
                loudnessGainMb = preset.loudnessMb.coerceIn(0, AudioEffectsController.MAX_LOUDNESS_GAIN_MB),
                reverbId = preset.reverb.id,
                reverbAmount = preset.reverbAmount.coerceIn(0, 100),
            )
        }
    }

    /** ثبت پریست جاری برای رده خروجی جاری (بلندگو/هدفون/بلوتوث/…) */
    private fun rememberForCurrentDevice(presetId: String) {
        val category = AudioEffectsHub.deviceCategory.value
        devicePersistJob?.cancel()
        devicePersistJob = viewModelScope.launch {
            settings.saveDeviceProfile(category.name, presetId)
        }
    }

    private fun publishAndPersist(transform: (EqState) -> EqState) {
        val next = transform(AudioEffectsHub.desiredState.value)
        AudioEffectsHub.setDesiredState(next)
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(400)
            val cap = AudioEffectsHub.capabilities.value
            settings.saveEqualizer(
                EqualizerSettings(
                    enabled = next.enabled,
                    profileId = next.profileId,
                    gainsDb = next.gainsDb,
                    bassStrength = next.bassStrength,
                    virtualizerStrength = next.virtualizerStrength,
                    loudnessGainMb = next.loudnessGainMb,
                    bandCount = if (cap.supported) cap.bandCount else 0,
                    reverbId = next.reverbId,
                    reverbAmount = next.reverbAmount,
                ),
            )
        }
    }
}
