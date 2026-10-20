package com.soundpilot.app.audio

/** قابلیت‌های واقعی اکولایزر دستگاه — در زمان اتصال به نشست صوتی تشخیص داده می‌شود */
data class EqualizerCapabilities(
    val supported: Boolean = false,
    val bandCount: Int = 0,
    val centersHz: List<Int> = emptyList(),
    val minDb: Float = -15f,
    val maxDb: Float = 15f,
    val bassSupported: Boolean = false,
    val virtualizerSupported: Boolean = false,
    val loudnessSupported: Boolean = false,
) {
    companion object {
        val UNSUPPORTED: EqualizerCapabilities = EqualizerCapabilities()
    }
}

/** وضعیت کامل افکت‌ها — هم برای اعمال همزمان و هم برای ذخیره‌سازی */
data class EqState(
    val enabled: Boolean = false,
    val profileId: String = "balanced",
    val gainsDb: List<Float> = emptyList(),
    val bassStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    /** بهره تقویت بلندی بر حسب میلی‌بِل (۰ تا ۱۵۰۰ = تا +۱۵ دسی‌بل) */
    val loudnessGainMb: Int = 0,
)

/**
 * پروفایل‌های صوتی به‌صورت منحنی (فرکانس → دسی‌بل) تعریف می‌شوند تا با هر تعداد باند
 * و هر محدوده فرکانسی روی دستگاه‌های مختلف درست نمونه‌برداری شوند.
 */
data class EqCurve(val id: String, val label: String, val points: List<Pair<Float, Float>>)

object EqualizerPresets {
    const val CUSTOM_ID = "custom"

    /** متعادل: تقویت بسیار ملایم بدون اعوجاج */
    val BALANCED = EqCurve(
        "balanced", "متعادل",
        listOf(60f to 1f, 200f to 0.5f, 1000f to 0f, 4000f to 0.5f, 12000f to 1f),
    )

    /**
     * خودرو: جبران نویز جاده — تقویت فرکانس‌های پایین (بیس در برابر صدای موتور)
     * و بالا (وضوح در برابر سروصدا) برای شنیده‌شدن طبیعی صدا روی ضبط ماشین.
     */
    val CAR = EqCurve(
        "car", "خودرو",
        listOf(60f to 4.5f, 150f to 3f, 400f to 0.5f, 1000f to 0f, 3000f to 1.5f, 8000f to 3f, 14000f to 2f),
    )

    /** بیس‌دار: تقویت متمرکز فرکانس‌های پایین برای ضبط ماشین */
    val BASS = EqCurve(
        "bass", "بیس‌دار",
        listOf(40f to 6f, 100f to 5f, 250f to 2.5f, 1000f to 0f, 4000f to 0.5f, 12000f to 1.5f),
    )

    /** وکال: وضوح صدای خواننده */
    val VOCAL = EqCurve(
        "vocal", "وکال",
        listOf(60f to -1.5f, 250f to 2f, 800f to 3f, 2500f to 2.5f, 6000f to 0.5f, 12000f to -0.5f),
    )

    /** شفافیت: بالا بردن جزئیات و فرکانس‌های بالا */
    val CLARITY = EqCurve(
        "clarity", "شفافیت",
        listOf(60f to -2f, 250f to -0.5f, 1000f to 1f, 4000f to 3f, 12000f to 4.5f),
    )

    /** راک: ضربه گیتار و انرژی مید‌رنج */
    val ROCK = EqCurve(
        "rock", "راک",
        listOf(60f to 5f, 200f to 2.5f, 1000f to -1f, 3000f to 3f, 6000f to 4f, 14000f to 3.5f),
    )

    /** پاپ: بیس ملایم و وکال درشت */
    val POP = EqCurve(
        "pop", "پاپ",
        listOf(100f to 3f, 300f to 1.5f, 1000f to 0f, 4000f to 2f, 12000f to 3f),
    )

    /** جاز: طیف گرم و متعادل */
    val JAZZ = EqCurve(
        "jazz", "جاز",
        listOf(60f to 3f, 250f to 2f, 1000f to 0f, 4000f to 2f, 12000f to 2.5f),
    )

    /** کلاسیک: پهنا و عمق ارکستر */
    val CLASSICAL = EqCurve(
        "classical", "کلاسیک",
        listOf(60f to 3f, 250f to 0f, 1000f to -0.5f, 4000f to 2f, 12000f to 3.5f),
    )

    /** الکترونیک: بیس عمیق و های‌هت‌های براق */
    val ELECTRONIC = EqCurve(
        "electronic", "الکترونیک",
        listOf(50f to 5.5f, 120f to 4.5f, 400f to 1f, 1000f to -0.5f, 6000f to 3f, 14000f to 4f),
    )

    val all: List<EqCurve> = listOf(
        BALANCED, CAR, BASS, VOCAL, CLARITY, ROCK, POP, JAZZ, CLASSICAL, ELECTRONIC,
    )

    fun byId(id: String): EqCurve = all.firstOrNull { it.id == id } ?: BALANCED
}
