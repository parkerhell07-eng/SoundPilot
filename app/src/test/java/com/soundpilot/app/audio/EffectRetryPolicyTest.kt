package com.soundpilot.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectRetryPolicyTest {

    // ---------------- backoff ----------------

    @Test
    fun `توالی عقب‌نشینی صعودی و محدود است`() {
        assertEquals(5, EffectRetryPolicy.MAX_ATTEMPTS)
        assertEquals(5, EffectRetryPolicy.BACKOFF_MS.size)
        // هر تأخیر ≥ قبلی و همه مثبت
        for (i in 1 until EffectRetryPolicy.BACKOFF_MS.size) {
            assertTrue(EffectRetryPolicy.BACKOFF_MS[i] >= EffectRetryPolicy.BACKOFF_MS[i - 1])
        }
        assertTrue(EffectRetryPolicy.BACKOFF_MS.first() > 0)
    }

    @Test
    fun `تاخیر تلاش‌ها به ترتیب سیاست است`() {
        assertEquals(500L, EffectRetryPolicy.delayBeforeAttempt(1))
        assertEquals(1_000L, EffectRetryPolicy.delayBeforeAttempt(2))
        assertEquals(2_000L, EffectRetryPolicy.delayBeforeAttempt(3))
        assertEquals(4_000L, EffectRetryPolicy.delayBeforeAttempt(4))
        // بیرون از بازه — امن clamp شود
        assertEquals(8_000L, EffectRetryPolicy.delayBeforeAttempt(5))
        assertEquals(8_000L, EffectRetryPolicy.delayBeforeAttempt(99))
        assertEquals(500L, EffectRetryPolicy.delayBeforeAttempt(0))
        assertEquals(500L, EffectRetryPolicy.delayBeforeAttempt(-3))
    }

    @Test
    fun `تلاش مجدد فقط تا قبل از سقف مجاز است`() {
        assertTrue(EffectRetryPolicy.shouldRetry(1))
        assertTrue(EffectRetryPolicy.shouldRetry(4))
        assertFalse(EffectRetryPolicy.shouldRetry(EffectRetryPolicy.MAX_ATTEMPTS))
        assertFalse(EffectRetryPolicy.shouldRetry(6))
        assertFalse(EffectRetryPolicy.shouldRetry(0))
        assertFalse(EffectRetryPolicy.shouldRetry(-1))
    }

    @Test
    fun `تاخیر جاافتادن مسیر صوتی کوتاه است`() {
        // نباید کاربر را معطل کند؛ زیر یک ثانیه
        assertTrue(EffectRetryPolicy.SETTLE_DELAY_MS in 1..999)
    }
}

class EffectFailureClassifierTest {

    @Test
    fun `پیام‌های شناخته‌شده audiofx دسته‌بندی می‌شوند`() {
        // درس‌گرفته از دستگاه واقعی (سامسونگ A33 / بلوتوث خودرو)
        assertEquals(
            EffectFailure.OUTPUT_NOT_READY_FOR_EFFECTS,
            EffectFailureClassifier.classify(RuntimeException("Effect bundle not found")),
        )
        assertEquals(
            EffectFailure.SESSION_NOT_READY,
            EffectFailureClassifier.classify(RuntimeException("init check failed!: -4")),
        )
        assertEquals(
            EffectFailure.SESSION_NOT_READY,
            EffectFailureClassifier.classify(RuntimeException("Error code -3")),
        )
        assertEquals(
            EffectFailure.SESSION_NOT_READY,
            EffectFailureClassifier.classify(RuntimeException("Binder invocation to an incorrect interface / dead object")),
        )
    }

    @Test
    fun `نوع استثنا هم کافی است`() {
        assertEquals(
            EffectFailure.SESSION_NOT_READY,
            EffectFailureClassifier.classify(IllegalStateException("no")),
        )
        assertEquals(
            EffectFailure.OUTPUT_NOT_READY_FOR_EFFECTS,
            EffectFailureClassifier.classify(UnsupportedOperationException("nope")),
        )
        assertEquals(
            EffectFailure.SESSION_NOT_READY,
            EffectFailureClassifier.classify(RuntimeException("weird")),
        )
        assertEquals(
            EffectFailure.GENERIC,
            EffectFailureClassifier.classify(Exception("weird")),
        )
    }

    @Test
    fun `ورودی خالی کرش نمی‌کند`() {
        assertEquals(EffectFailure.GENERIC, EffectFailureClassifier.classify(null))
        // RuntimeException با پیام null هم طبق طراحی → SESSION_NOT_READY
        assertEquals(
            EffectFailure.SESSION_NOT_READY,
            EffectFailureClassifier.classify(RuntimeException(null as String?)),
        )
        // Exception عمومی با پیام null → GENERIC
        assertEquals(
            EffectFailure.GENERIC,
            EffectFailureClassifier.classify(Exception(null as String?)),
        )
    }

    @Test
    fun `پیام کاربر برای همه دسته‌ها موجود است`() {
        EffectFailure.entries.forEach { failure ->
            val msg = EffectFailureClassifier.userMessage(failure)
            assertTrue(msg.isNotBlank())
            // پیام فارسی است و هیچ اصطلاح فنی انگلیسی مستقیم ندارد
            assertFalse(msg.contains("Effect"))
            assertFalse(msg.contains("session", ignoreCase = true))
        }
    }
}
