package com.soundpilot.app.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    /**
     * ساعت = زمان مجازی TestScope؛ چون تایمر در backgroundScope همان اسکوپ اجرا می‌شود،
     * تأخیرهای مجازی و ساعت دقیقاً هم‌گام‌اند و نتیجه کاملاً قطعی است.
     */
    @Test
    fun `countdown fires onFinish once at zero and resets state`() = runTest {
        val timer = SleepTimer(scope = backgroundScope, tickMs = 1_000, elapsedMs = { currentTime })
        var finishCount = 0
        timer.start(5_000) { finishCount++ }

        assertEquals(SleepTimer.Mode.COUNTDOWN, timer.state.value.mode)
        assertEquals(5_000L, timer.state.value.remainingMs)

        advanceTimeBy(3_000)
        runCurrent()
        assertTrue("باقی‌مانده باید حدود ۲ ثانیه باشد: ${timer.state.value.remainingMs}",
            timer.state.value.remainingMs in 1..2_000)

        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(1, finishCount)
        assertEquals(SleepTimer.Mode.OFF, timer.state.value.mode)
        assertEquals(0L, timer.state.value.remainingMs)
    }

    @Test
    fun `cancel stops countdown without firing onFinish`() = runTest {
        val timer = SleepTimer(scope = backgroundScope, tickMs = 1_000, elapsedMs = { currentTime })
        var finishCount = 0
        timer.start(10_000) { finishCount++ }

        advanceTimeBy(2_000)
        runCurrent()
        timer.cancel()

        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(0, finishCount)
        assertEquals(SleepTimer.Mode.OFF, timer.state.value.mode)
    }

    @Test
    fun `restart replaces previous timer`() = runTest {
        val timer = SleepTimer(scope = backgroundScope, tickMs = 1_000, elapsedMs = { currentTime })
        var finishCount = 0
        timer.start(10_000) { finishCount++ }
        advanceTimeBy(1_100)
        runCurrent()

        timer.start(60_000) { finishCount++ }
        advanceTimeBy(9_100)
        runCurrent()
        // تایمر قبلی (که تا ۱۰ ثانیه باقی می‌ماند) نباید فعّال باشد
        assertEquals(0, finishCount)
        assertTrue(timer.state.value.remainingMs > 50_000)

        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, finishCount)
    }

    @Test
    fun `endOfTrack mode waits for explicit finish`() = runTest {
        val timer = SleepTimer(scope = backgroundScope, tickMs = 1_000, elapsedMs = { currentTime })
        var finishCount = 0
        timer.markEndOfTrack()

        assertEquals(SleepTimer.Mode.END_OF_TRACK, timer.state.value.mode)
        advanceTimeBy(120_000)
        runCurrent()
        assertEquals(0, finishCount)

        timer.finishEndOfTrack { finishCount++ }
        assertEquals(1, finishCount)
        assertEquals(SleepTimer.Mode.OFF, timer.state.value.mode)
    }

    @Test
    fun `finishEndOfTrack is no-op in countdown mode`() = runTest {
        val timer = SleepTimer(scope = backgroundScope, tickMs = 1_000, elapsedMs = { currentTime })
        var finishCount = 0
        timer.start(30_000) { finishCount++ }
        timer.finishEndOfTrack { finishCount++ }
        assertEquals(0, finishCount)
        assertEquals(SleepTimer.Mode.COUNTDOWN, timer.state.value.mode)
    }

    @Test
    fun `zero duration start is rejected`() = runTest {
        val timer = SleepTimer(scope = backgroundScope, tickMs = 1_000, elapsedMs = { currentTime })
        var finishCount = 0
        timer.start(0) { finishCount++ }
        assertEquals(SleepTimer.Mode.OFF, timer.state.value.mode)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(0, finishCount)
    }
}
