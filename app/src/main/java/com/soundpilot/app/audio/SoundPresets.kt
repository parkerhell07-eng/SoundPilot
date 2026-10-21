package com.soundpilot.app.audio

/**
 * سطح «سیستم صوتی مقصد» — هر پریست برای یک رده از سیستم‌ها تنظیم شده است.
 * این کمک می‌کند کاربر بدون دانش فنی، پریست مناسب خودرو/گوشی خود را پیدا کند.
 */
enum class SoundTier(val label: String) {
    ALL("همه سیستم‌ها"),
    WEAK("سیستم ضعیف"),
    MEDIUM("سیستم متوسط"),
    STRONG("سیستم قوی"),
}

/**
 * presetهای افکت طنین (Reverb) سخت‌افزاری اندروید.
 * مقادیر id دقیقاً همان ثابت‌های PresetReverb اندروید است.
 */
enum class ReverbPreset(val id: Int, val label: String) {
    OFF(0, "بدون طنین"),
    SMALL_ROOM(1, "اتاق کوچک"),
    MEDIUM_ROOM(2, "اتاق متوسط"),
    LARGE_ROOM(3, "اتاق بزرگ"),
    MEDIUM_HALL(4, "تالار متوسط"),
    LARGE_HALL(5, "تالار بزرگ"),
    PLATE(6, "پلیت استودیویی"),
    ;

    companion object {
        fun fromId(id: Int): ReverbPreset = entries.firstOrNull { it.id == id } ?: OFF
    }
}

/**
 * پریست کامل صدا: منحنی اکولایزر + بیس + فراگیر + بلندی + طنین.
 *
 * هر پریست یک «ترکیب آمادهٔ هماهنگ» است؛ به‌جای اینکه کاربر ۵ اسلایدر را
 * خودش تنظیم کند، یک سناریوی صوتی کامل را با یک لمس اعمال می‌کند.
 * منحنی‌ها به‌صورت نقطه‌ای (فرکانس → دسی‌بل) تعریف می‌شوند تا با هر تعداد
 * باند روی هر دستگاهی درست نمونه‌برداری شوند.
 */
data class SoundPreset(
    val id: String,
    val label: String,
    val subtitle: String,
    val tier: SoundTier,
    val curve: List<Pair<Float, Float>>,
    val bass: Int,
    val surround: Int,
    val loudnessMb: Int,
    val reverb: ReverbPreset = ReverbPreset.OFF,
    val reverbAmount: Int = 0,
)

object SoundPresets {

    // ───────────────────────────── مرجع و خودرو ─────────────────────────────

    /** مرجع خنثی: هیچ تغییری در صدای اصلی — نقطه شروع مقایسه A/B */
    val FLAT = SoundPreset(
        id = "flat", label = "متعادل", subtitle = "صدای خالص و بدون تغییر — مرجع مقایسه",
        tier = SoundTier.ALL,
        curve = listOf(60f to 0f, 200f to 0f, 1000f to 0f, 4000f to 0f, 12000f to 0f),
        bass = 0, surround = 0, loudnessMb = 0,
    )

    /** خودروی اقتصادی: اسپیکرهای کوچک و نویز جاده — بیس و وضوح جبران می‌شود */
    val CAR_ECONOMY = SoundPreset(
        id = "car-economy", label = "خودروی اقتصادی", subtitle = "جبران نویز جاده روی اسپیکرهای کوچک ضبط",
        tier = SoundTier.WEAK,
        curve = listOf(60f to 4.5f, 150f to 3f, 400f to 0.5f, 1000f to 0f, 3000f to 1.5f, 8000f to 3f, 14000f to 2f),
        bass = 250, surround = 0, loudnessMb = 500,
    )

    /** خودروی استاندارد: تعادل بین بیس، وکال و وضوح روی ضبط معمولی */
    val CAR_STANDARD = SoundPreset(
        id = "car-standard", label = "خودروی استاندارد", subtitle = "تعادل کامل برای ضبط کارخانه‌ای معمولی",
        tier = SoundTier.MEDIUM,
        curve = listOf(50f to 4f, 120f to 3f, 400f to 0.5f, 1000f to 0f, 3000f to 1.5f, 8000f to 2.5f, 14000f to 1.5f),
        bass = 400, surround = 150, loudnessMb = 300,
    )

    /** خودروی حرفه‌ای: سیستم چند‌کاناله — بیس عمیق بدون افت وضوح */
    val CAR_PRO = SoundPreset(
        id = "car-pro", label = "خودروی حرفه‌ای", subtitle = "برای سیستم‌های آمپلیفایر و ساب‌ووفر",
        tier = SoundTier.STRONG,
        curve = listOf(40f to 5.5f, 100f to 3.5f, 300f to 0.5f, 1000f to 0f, 3000f to 1f, 8000f to 2.5f, 14000f to 2.5f),
        bass = 600, surround = 250, loudnessMb = 0,
    )

    // ───────────────────────────── بیس ─────────────────────────────

    /** بیس دیوانه: حداکثر فشار فرکانس پایین برای طرفداران EDM و رپ */
    val BASS_INSANE = SoundPreset(
        id = "bass-insane", label = "بیس دیوانه", subtitle = "حداکثر فشار بیس — احساس ضربه در سینه",
        tier = SoundTier.MEDIUM,
        curve = listOf(40f to 6f, 100f to 5.5f, 250f to 2.5f, 1000f to -1f, 4000f to 0.5f, 12000f to 1.5f),
        bass = 900, surround = 0, loudnessMb = 200,
    )

    /** بیس تمیز: بیس محسوس اما کنترل‌شده بدون گرفتگی میدرنج */
    val BASS_CLEAN = SoundPreset(
        id = "bass-clean", label = "بیس تمیز", subtitle = "بیس محسوس بدون گرفتگی صدای وسط",
        tier = SoundTier.ALL,
        curve = listOf(50f to 4f, 120f to 3f, 300f to 1f, 1000f to 0f, 4000f to 0.5f, 12000f to 1f),
        bass = 500, surround = 0, loudnessMb = 0,
    )

    /** ساب‌ووفر: تمرکز کامل روی زیربیس — مخصوص سیستم مجهز به ساب */
    val SUBWOOFER = SoundPreset(
        id = "subwoofer", label = "ساب‌ووفر", subtitle = "تمرکز روی زیربیس برای سیستم مجهز به ساب",
        tier = SoundTier.STRONG,
        curve = listOf(40f to 6f, 80f to 5f, 200f to 1.5f, 1000f to -0.5f, 6000f to 1f, 14000f to 1f),
        bass = 1000, surround = 0, loudnessMb = 0,
    )

    // ───────────────────────────── ژانرها ─────────────────────────────

    val POP = SoundPreset(
        id = "pop", label = "پاپ", subtitle = "وکال درشت و بیس ملایم — امضای پاپ",
        tier = SoundTier.ALL,
        curve = listOf(100f to 3f, 300f to 1.5f, 1000f to 0f, 4000f to 2f, 12000f to 3f),
        bass = 200, surround = 100, loudnessMb = 0,
    )

    val ROCK = SoundPreset(
        id = "rock", label = "راک", subtitle = "انرژی گیتار و ضربه درامز",
        tier = SoundTier.ALL,
        curve = listOf(60f to 5f, 200f to 2.5f, 1000f to -1f, 3000f to 3f, 6000f to 4f, 14000f to 3.5f),
        bass = 300, surround = 150, loudnessMb = 0,
    )

    val METAL = SoundPreset(
        id = "metal", label = "متال", subtitle = "دابل‌کیک تیز و ریتم سنگین",
        tier = SoundTier.MEDIUM,
        curve = listOf(60f to 4.5f, 150f to 3f, 500f to 0f, 2000f to 2.5f, 5000f to 4f, 12000f to 4f),
        bass = 350, surround = 200, loudnessMb = 0,
    )

    val JAZZ = SoundPreset(
        id = "jazz", label = "جاز", subtitle = "طیف گرم با فضای کوچک کلاب",
        tier = SoundTier.ALL,
        curve = listOf(60f to 3f, 250f to 2f, 1000f to 0f, 4000f to 2f, 12000f to 2.5f),
        bass = 200, surround = 150, loudnessMb = 0, reverb = ReverbPreset.SMALL_ROOM, reverbAmount = 20,
    )

    val CLASSICAL = SoundPreset(
        id = "classical", label = "کلاسیک", subtitle = "عمق و پهنای سالن ارکستر",
        tier = SoundTier.ALL,
        curve = listOf(60f to 3f, 250f to 0f, 1000f to -0.5f, 4000f to 2f, 12000f to 3.5f),
        bass = 100, surround = 200, loudnessMb = 0, reverb = ReverbPreset.MEDIUM_HALL, reverbAmount = 30,
    )

    val EDM = SoundPreset(
        id = "edm", label = "الکترونیک", subtitle = "دراپ عمیق و های‌هت براق",
        tier = SoundTier.ALL,
        curve = listOf(50f to 5.5f, 120f to 4.5f, 400f to 1f, 1000f to -0.5f, 6000f to 3f, 14000f to 4f),
        bass = 500, surround = 250, loudnessMb = 200,
    )

    val HIPHOP = SoundPreset(
        id = "hiphop", label = "هیپ‌هاپ", subtitle = "۸۰۸ سنگین با وکال جلو",
        tier = SoundTier.ALL,
        curve = listOf(50f to 5f, 100f to 4f, 250f to 1.5f, 1000f to 1f, 4000f to 1.5f, 12000f to 2f),
        bass = 550, surround = 100, loudnessMb = 0,
    )

    val ACOUSTIC = SoundPreset(
        id = "acoustic", label = "آکوستیک", subtitle = "گرمای گیتار چوبی و فضای طبیعی",
        tier = SoundTier.ALL,
        curve = listOf(80f to 2f, 250f to 1.5f, 1000f to 0.5f, 3000f to 2.5f, 8000f to 2f),
        bass = 150, surround = 150, loudnessMb = 0, reverb = ReverbPreset.MEDIUM_ROOM, reverbAmount = 25,
    )

    // ───────────────────────────── گفتار و شرایط خاص ─────────────────────────────

    val VOCAL = SoundPreset(
        id = "vocal", label = "وکال و پادکست", subtitle = "حداکثر وضوح صدای خواننده و گفتار",
        tier = SoundTier.ALL,
        curve = listOf(60f to -1.5f, 250f to 2f, 800f to 3f, 2500f to 2.5f, 6000f to 0.5f, 12000f to -0.5f),
        bass = 0, surround = 0, loudnessMb = 0,
    )

    /** شبانه: شنیدن با صدای کم — جزئیات بدون نیاز به بلندکردن ولوم */
    val LATE_NIGHT = SoundPreset(
        id = "late-night", label = "شبانه", subtitle = "شنیدن با صدای کم بدون حذف جزئیات",
        tier = SoundTier.ALL,
        curve = listOf(60f to 2f, 200f to 1.5f, 1000f to 1.5f, 4000f to 2f, 12000f to 0.5f),
        bass = 100, surround = 0, loudnessMb = 900,
    )

    // ───────────────────────────── فضاسازی ─────────────────────────────

    val SPATIAL_3D = SoundPreset(
        id = "spatial3d", label = "فضاسازی سه‌بعدی", subtitle = "صدا فراتر از اسپیکرها — تجربه فراگیر",
        tier = SoundTier.STRONG,
        curve = listOf(60f to 2f, 250f to 0.5f, 1000f to 0f, 4000f to 1.5f, 12000f to 2f),
        bass = 200, surround = 900, loudnessMb = 0, reverb = ReverbPreset.LARGE_HALL, reverbAmount = 35,
    )

    val CONCERT = SoundPreset(
        id = "concert", label = "کنسرت زنده", subtitle = "حس سالن بزرگ و جمعیت زنده",
        tier = SoundTier.MEDIUM,
        curve = listOf(60f to 3f, 200f to 1.5f, 1000f to 0f, 4000f to 1f, 12000f to 1.5f),
        bass = 250, surround = 400, loudnessMb = 0, reverb = ReverbPreset.LARGE_HALL, reverbAmount = 45,
    )

    val CRYSTAL = SoundPreset(
        id = "crystal", label = "شفافیت کریستالی", subtitle = "جزئیات درخشان فرکانس‌های بالا",
        tier = SoundTier.MEDIUM,
        curve = listOf(60f to -2f, 250f to -0.5f, 1000f to 1f, 4000f to 3f, 12000f to 4.5f),
        bass = 0, surround = 100, loudnessMb = 0,
    )

    /** همه ۲۰ پریست به ترتیب نمایش در استودیو */
    val all: List<SoundPreset> = listOf(
        FLAT, CAR_ECONOMY, CAR_STANDARD, CAR_PRO,
        BASS_INSANE, BASS_CLEAN, SUBWOOFER,
        POP, ROCK, METAL, JAZZ, CLASSICAL, EDM, HIPHOP, ACOUSTIC,
        VOCAL, LATE_NIGHT,
        SPATIAL_3D, CONCERT, CRYSTAL,
    )

    /** نگاشت idهای نسخه‌های قبلی به پریست‌های جدید — سازگاری تنظیمات ذخیره‌شده */
    private val legacyMap = mapOf(
        "balanced" to FLAT.id,
        "car" to CAR_STANDARD.id,
        "bass" to BASS_CLEAN.id,
        "clarity" to CRYSTAL.id,
        "electronic" to EDM.id,
    )

    fun byId(id: String?): SoundPreset {
        if (id == null) return FLAT
        val mapped = legacyMap[id] ?: id
        return all.firstOrNull { it.id == mapped } ?: FLAT
    }
}
