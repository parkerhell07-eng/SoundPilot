package com.soundpilot.app.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * تایمر خواب — منطق خالص و قابل تست واحد.
 *
 * دو حالت:
 *  - COUNTDOWN: پس از مدت مشخص، [onFinish] صدا زده می‌شود (پاز پخش).
 *  - END_OF_TRACK: «پایان همین آهنگ» — پایان آن با تشخیص انتهای آهنگ جاری
 *    از سمت ViewModel اعلام و در همان لحظه [onFinish] صدا زده می‌شود.
 *
 * ساعت با [elapsedMs] تزریق می‌شود تا در تست با زمان مجازی کنترل شود.
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val tickMs: Long = 1_000L,
    private val elapsedMs: () -> Long = System::currentTimeMillis,
) {

    enum class Mode { OFF, COUNTDOWN, END_OF_TRACK }

    data class State(
        val mode: Mode = Mode.OFF,
        /** زمان باقی‌مانده تا پاز؛ در حالت OFF/END_OF_TRACK صفر است */
        val remainingMs: Long = 0L,
        /** کل مدت انتخاب‌شده — برای نمایش نوار پیشرفت */
        val totalMs: Long = 0L,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var job: Job? = null

    /** شروع شمارش معکوس؛ تایمر قبلی در صورت وجود لغو می‌شود */
    fun start(durationMs: Long, onFinish: () -> Unit) {
        cancel()
        val duration = durationMs.coerceAtLeast(0)
        if (duration == 0L) return
        _state.value = State(Mode.COUNTDOWN, remainingMs = duration, totalMs = duration)
        job = scope.launch {
            val endAt = elapsedMs() + duration
            while (true) {
                val remaining = endAt - elapsedMs()
                if (remaining <= 0) break
                _state.value = State(Mode.COUNTDOWN, remainingMs = remaining, totalMs = duration)
                delay(min(tickMs, remaining))
            }
            _state.value = State()
            onFinish()
        }
    }

    /** حالت «پایان همین آهنگ» — پایان واقعی با [finishEndOfTrack] اعلام می‌شود */
    fun markEndOfTrack() {
        cancelCountdown()
        _state.value = State(Mode.END_OF_TRACK)
    }

    /** پایان آهنگ جاری تشخیص داده شد → اجرای عمل (پاز) و ریست */
    fun finishEndOfTrack(onFinish: () -> Unit) {
        if (_state.value.mode != Mode.END_OF_TRACK) return
        cancel()
        onFinish()
    }

    /** لغو کامل تایمر */
    fun cancel() {
        job?.cancel()
        job = null
        _state.value = State()
    }

    private fun cancelCountdown() {
        job?.cancel()
        job = null
    }
}
