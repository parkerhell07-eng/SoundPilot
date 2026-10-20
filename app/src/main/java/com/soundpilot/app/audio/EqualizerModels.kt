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

    val all: List<EqCurve> = listOf(BALANCED, BASS, VOCAL, CLARITY)

    fun byId(id: String): EqCurve = all.firstOrNull { it.id == id } ?: BALANCED
}
