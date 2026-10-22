package com.soundpilot.app.audio

/**
 * سیاست تلاش مجدد برای اتصال افکت‌های صوتی — منطق خالص و قابل تست واحد.
 *
 * چرا لازم است؟ روی برخی دستگاه‌ها (به‌ویژه سامسونگ با مسیر بلوتوث A2DP)،
 * ایجاد Equalizer در همان لحظهٔ تغییر خروجی یا قبل از آماده‌شدن کامل نشست صوتی
 * شکست می‌خورد («Effect bundle not found» یا «init check failed»). اگر فقط یک بار
 * تلاش شود، کاربر برای همیشه پیام «سازگار نیست» می‌بیند؛ در حالی که چند لحظه بعد
 * همان افکت بدون مشکل وصل می‌شود.
 *
 * پس موتور حرفه‌ای باید: عقب‌نشینی زمانی (backoff) + تلاش مجدد خودکار + تشخیص نوع خطا داشته باشد.
 */
object EffectRetryPolicy {

    /** حداکثر تعداد تلاش برای هر بار اتصال (شامل تلاش اول) */
    const val MAX_ATTEMPTS = 5

    /** توالی تأخیر بر حسب میلی‌ثانیه: ۰٫۵s → ۱s → ۲s → ۴s → ۸s */
    val BACKOFF_MS = longArrayOf(500L, 1_000L, 2_000L, 4_000L, 8_000L)

    /** آیا برای این شماره تلاش (از ۱ شروع می‌شود) اجازه تلاش مجدد هست؟ */
    fun shouldRetry(attempt: Int): Boolean = attempt in 1 until MAX_ATTEMPTS

    /** تأخیر قبل از تلاش مجدد برای شماره تلاش داده‌شده (از ۱) */
    fun delayBeforeAttempt(attempt: Int): Long {
        val index = (attempt - 1).coerceIn(0, BACKOFF_MS.lastIndex)
        return BACKOFF_MS[index]
    }

    /**
     * تأخیر کوتاه «ملایم‌سازی» پس از رویدادها (شروع پخش/تغییر خروجی) تا سیستم
     * صوتی فرصت جاافتادن مسیر جدید را داشته باشد — تجربه‌ای که روی دستگاه‌های
     * کندتر تفاوت واقعی ایجاد می‌کند.
     */
    const val SETTLE_DELAY_MS = 300L
}

/**
 * دسته‌بندی خطای اتصال افکت — خالص و قابل تست.
 * پیام‌های متعارف audiofx اندروید را به دلیل انسانی تبدیل می‌کند.
 */
enum class EffectFailure {
    /** نشست صوتی هنوز کاملاً آماده نیست — تلاش مجدد تقریباً همیشه موفق است */
    SESSION_NOT_READY,

    /** این خروجی/روم افکت را نمی‌پذیرد — ولی با تغییر خروجی یا زمان ممکن است شود */
    OUTPUT_NOT_READY_FOR_EFFECTS,

    /** خطای نامشخص — تلاش مجدد منطقی است */
    GENERIC,
}

object EffectFailureClassifier {

    /**
     * دسته‌بندی از روی نوع استثنا و متن آن.
     * متن‌های شناخته‌شده audiofx:
     * - «Effect bundle not found» → کتابخانه افکت روی این مسیر صدا لود نشده
     * - «init check failed» / «Error code -4» → نشست یا موتور هنوز آماده نیست
     * - «dead object» → سرویس صوتی سیستم در حال بازراه‌اندازی است
     */
    fun classify(exception: Throwable?, message: String? = exception?.message): EffectFailure {
        val text = (message ?: "").lowercase()
        return when {
            text.contains("effect bundle not found") -> EffectFailure.OUTPUT_NOT_READY_FOR_EFFECTS
            text.contains("init check failed") -> EffectFailure.SESSION_NOT_READY
            text.contains("error code") || text.contains("error -") -> EffectFailure.SESSION_NOT_READY
            text.contains("dead object") || text.contains("deadObject".lowercase()) ->
                EffectFailure.SESSION_NOT_READY
            text.contains("state") && text.contains("uninitialized") -> EffectFailure.SESSION_NOT_READY
            exception is IllegalStateException -> EffectFailure.SESSION_NOT_READY
            exception is UnsupportedOperationException -> EffectFailure.OUTPUT_NOT_READY_FOR_EFFECTS
            exception is RuntimeException -> EffectFailure.SESSION_NOT_READY
            else -> EffectFailure.GENERIC
        }
    }

    /** پیام کاربرپسند فارسی برای هر دسته — بدون اصطلاح فنی */
    fun userMessage(failure: EffectFailure): String = when (failure) {
        EffectFailure.SESSION_NOT_READY ->
            "اتصال افکت‌ها به موتور صدا چند لحظه طول کشید؛ به‌محض آماده‌شدن دوباره وصل می‌شود."
        EffectFailure.OUTPUT_NOT_READY_FOR_EFFECTS ->
            "این خروجی صوتی هنوز افکت‌ها را نمی‌پذیرد؛ با تغییر خروجی یا تلاش مجدد معمولاً حل می‌شود."
        EffectFailure.GENERIC ->
            "اتصال افکت‌ها ناموفق بود؛ می‌توانید دوباره تلاش کنید."
    }
}
