package com.soundpilot.app.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundpilot.app.audio.AudioEffectsHub
import com.soundpilot.app.audio.EqState
import com.soundpilot.app.audio.EqualizerMath
import com.soundpilot.app.audio.EqualizerPresets
import com.soundpilot.app.data.EqualizerSettings
import com.soundpilot.app.di.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    val bassSupported: Boolean = false,
    val virtualizerSupported: Boolean = false,
)

/**
 * پل بین صفحه اکولایزر و افکت‌های واقعی صوتی.
 * تغییرها بلافاصله روی نشست صوتی زنده اعمال و با تأخیر کوتاه در DataStore ذخیره می‌شوند.
 */
class EqualizerViewModel : ViewModel() {

    private val settings = ServiceLocator.settingsRepository
    private var persistJob: Job? = null

    val uiState: StateFlow<EqualizerUiState> = combine(
        AudioEffectsHub.capabilities,
        AudioEffectsHub.sessionActive,
        AudioEffectsHub.desiredState,
    ) { cap, active, desired ->
        val gains = when {
            cap.supported && desired.gainsDb.size == cap.bandCount -> desired.gainsDb
            cap.supported -> EqualizerMath.gainsFor(
                EqualizerPresets.byId(desired.profileId),
                cap.centersHz,
                cap.minDb,
                cap.maxDb,
            )
            else -> emptyList()
        }
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
            bassSupported = cap.bassSupported,
            virtualizerSupported = cap.virtualizerSupported,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EqualizerUiState())

    fun setEnabled(value: Boolean) = publishAndPersist { it.copy(enabled = value) }

    fun setProfile(profileId: String) {
        val cap = AudioEffectsHub.capabilities.value
        val gains = if (cap.supported) {
            EqualizerMath.gainsFor(
                EqualizerPresets.byId(profileId),
                cap.centersHz,
                cap.minDb,
                cap.maxDb,
            )
        } else {
            emptyList()
        }
        publishAndPersist { it.copy(profileId = profileId, gainsDb = gains) }
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

    fun reset() {
        val cap = AudioEffectsHub.capabilities.value
        val gains = if (cap.supported) {
            EqualizerMath.gainsFor(EqualizerPresets.BALANCED, cap.centersHz, cap.minDb, cap.maxDb)
        } else {
            emptyList()
        }
        publishAndPersist {
            it.copy(
                profileId = EqualizerPresets.BALANCED.id,
                gainsDb = gains,
                bassStrength = 0,
                virtualizerStrength = 0,
            )
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
                    bandCount = if (cap.supported) cap.bandCount else 0,
                ),
            )
        }
    }
}
