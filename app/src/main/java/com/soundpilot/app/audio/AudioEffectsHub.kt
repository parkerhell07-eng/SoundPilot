package com.soundpilot.app.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * نقطه اشتراک بین سرویس پخش (مالک افکت‌ها) و لایه UI.
 * چون سرویس و UI در یک پروسه اجرا می‌شوند، این Singleton امن است.
 */
object AudioEffectsHub {

    private val _capabilities = MutableStateFlow(EqualizerCapabilities.UNSUPPORTED)
    val capabilities: StateFlow<EqualizerCapabilities> = _capabilities.asStateFlow()

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive.asStateFlow()

    private val _desiredState = MutableStateFlow(EqState())
    val desiredState: StateFlow<EqState> = _desiredState.asStateFlow()

    @Volatile
    private var controller: AudioEffectsController? = null

    /** فقط توسط PlaybackService فراخوانی می‌شود */
    fun attachController(controller: AudioEffectsController?) {
        this.controller = controller
    }

    /** فقط توسط AudioEffectsController فراخوانی می‌شود */
    fun publish(capabilities: EqualizerCapabilities, sessionActive: Boolean) {
        _capabilities.value = capabilities
        _sessionActive.value = sessionActive
    }

    /**
     * تغییر وضعیت افکت‌ها از سمت UI:
     * وضعیت منتشر و بلافاصله روی افکت‌های زنده (در صورت وجود) اعمال می‌شود.
     */
    fun setDesiredState(state: EqState) {
        _desiredState.value = state
        controller?.apply(state)
    }
}
