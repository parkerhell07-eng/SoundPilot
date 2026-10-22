package com.soundpilot.app.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** وضعیت موتور افکت‌های صوتی — برای نمایش دقیق و قابل‌اعتماد در UI */
sealed interface EffectsStatus {
    /** سرویس پخش هنوز اجرا نشده */
    data object Idle : EffectsStatus

    /** در حال اتصال افکت‌ها (شامل تلاش‌های مجدد خودکار) */
    data class Preparing(val attempt: Int) : EffectsStatus

    /** افکت‌ها روی نشست پخش زنده متصل‌اند */
    data class Ready(val capabilities: EqualizerCapabilities) : EffectsStatus

    /**
     * اتصال پس از همه تلاش‌های خودکار ناموفق ماند.
     * [retryable] همیشه true است چون کاربر همیشه می‌تواند دستی تلاش کند؛
     * [message] دلیل انسانی برای نمایش مستقیم.
     */
    data class Unavailable(val message: String, val retryable: Boolean = true) : EffectsStatus
}

/**
 * نقطه اشتراک بین سرویس پخش (مالک افکت‌ها) و لایه UI.
 * چون سرویس و UI در یک پروسه اجرا می‌شوند، این Singleton امن است.
 */
object AudioEffectsHub {

    private val _status = MutableStateFlow<EffectsStatus>(EffectsStatus.Idle)
    val status: StateFlow<EffectsStatus> = _status.asStateFlow()

    /** سازگاری با نسخه‌های قبل — از وضعیت کامل مشتق می‌شود */
    private val _capabilities = MutableStateFlow(EqualizerCapabilities.UNSUPPORTED)
    val capabilities: StateFlow<EqualizerCapabilities> = _capabilities.asStateFlow()

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive: StateFlow<Boolean> = _sessionActive.asStateFlow()

    private val _desiredState = MutableStateFlow(EqState())
    val desiredState: StateFlow<EqState> = _desiredState.asStateFlow()

    /** رده خروجی صوتی فعلی (بلندگو/هدفون/بلوتوث/…) — توسط کنترل‌کننده منتشر می‌شود */
    private val _deviceCategory = MutableStateFlow(AudioDeviceCategory.SPEAKER)
    val deviceCategory: StateFlow<AudioDeviceCategory> = _deviceCategory.asStateFlow()

    @Volatile
    private var controller: AudioEffectsController? = null

    /** فقط توسط PlaybackService فراخوانی می‌شود */
    fun attachController(controller: AudioEffectsController?) {
        this.controller = controller
    }

    /** فقط توسط AudioEffectsController فراخوانی می‌شود */
    fun publishStatus(status: EffectsStatus) {
        _status.value = status
        when (status) {
            is EffectsStatus.Ready -> {
                _capabilities.value = status.capabilities
                _sessionActive.value = true
            }
            is EffectsStatus.Preparing -> {
                _sessionActive.value = true
            }
            EffectsStatus.Idle,
            is EffectsStatus.Unavailable,
            -> {
                _capabilities.value = EqualizerCapabilities.UNSUPPORTED
                _sessionActive.value = false
            }
        }
    }

    /** فقط توسط AudioEffectsController فراخوانی می‌شود */
    fun publishDeviceCategory(category: AudioDeviceCategory) {
        _deviceCategory.value = category
    }

    /**
     * تغییر وضعیت افکت‌ها از سمت UI:
     * وضعیت منتشر و بلافاصله روی افکت‌های زنده (در صورت وجود) اعمال می‌شود.
     */
    fun setDesiredState(state: EqState) {
        _desiredState.value = state
        controller?.apply(state)
    }

    /** درخواست تلاش مجدد اتصال افکت‌ها از سمت UI (دکمه «تلاش مجدد») */
    fun requestRetry() {
        controller?.retryFromUser()
    }
}
