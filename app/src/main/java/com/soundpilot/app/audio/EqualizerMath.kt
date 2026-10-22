package com.soundpilot.app.audio

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * ریاضیات اکولایزر — منطق خالص و قابل تست واحد.
 * منحنی پروفایل‌ها در مقیاس لگاریتمی فرکانس درون‌یابی می‌شود.
 */
object EqualizerMath {

    /** سقف تقویت پروفایل‌های آماده برای جلوگیری از اعوجاج */
    const val MAX_PRESET_BOOST_DB = 6f

    /** سقف تقویت دستی کاربر (کمتر از سقف سخت‌افزاری) */
    const val MAX_USER_BOOST_DB = 10f

    /** درون‌یابی خطی منحنی در مقیاس لگاریتمی فرکانس */
    fun sampleCurve(points: List<Pair<Float, Float>>, freqHz: Float): Float {
        if (points.isEmpty()) return 0f
        val sorted = points.sortedBy { ln(it.first.coerceAtLeast(1f).toDouble()) }
        val x = ln(freqHz.coerceAtLeast(1f).toDouble())

        val (firstFreq, firstGain) = sorted.first()
        val (lastFreq, lastGain) = sorted.last()
        if (x <= ln(firstFreq.coerceAtLeast(1f).toDouble())) return firstGain
        if (x >= ln(lastFreq.coerceAtLeast(1f).toDouble())) return lastGain

        for (i in 0 until sorted.size - 1) {
            val (f1, g1) = sorted[i]
            val (f2, g2) = sorted[i + 1]
            val x1 = ln(f1.coerceAtLeast(1f).toDouble())
            val x2 = ln(f2.coerceAtLeast(1f).toDouble())
            if (x >= x1 && x <= x2) {
                if (x2 == x1) return g2
                val t = ((x - x1) / (x2 - x1)).toFloat()
                return g1 + t * (g2 - g1)
            }
        }
        return 0f
    }

    /** محاسبه بهره هر باند بر اساس منحنی پروفایل و باندهای واقعی دستگاه */
    fun gainsFor(
        curve: EqCurve,
        centersHz: List<Int>,
        minDb: Float,
        maxDb: Float,
    ): List<Float> = centersHz.map { freq ->
        sampleCurve(curve.points, freq.toFloat())
            .coerceIn(minDb, minOf(maxDb, MAX_PRESET_BOOST_DB))
    }

    /**
     * درون‌یابی خطی سطوح واقعی باندها در مقیاس لگاریتمی فرکانس — برای رسم
     * «نمودار زنده» اکولایزر که دقیقاً همان چیزی است که کاربر با فیدرها ساخته.
     * فراتر از اولین/آخرین باند، سطح همان باند لبه حفظ می‌شود.
     */
    fun interpolateGains(centersHz: List<Int>, gainsDb: List<Float>, freqHz: Float): Float {
        val n = minOf(centersHz.size, gainsDb.size)
        if (n == 0) return 0f
        if (n == 1) return gainsDb[0]
        val x = ln(freqHz.coerceAtLeast(1f).toDouble())
        val xs = List(n) { ln(centersHz[it].coerceAtLeast(1).toDouble()) }
        if (x <= xs.first()) return gainsDb[0]
        if (x >= xs[n - 1]) return gainsDb[n - 1]
        for (i in 0 until n - 1) {
            if (x >= xs[i] && x <= xs[i + 1]) {
                val span = xs[i + 1] - xs[i]
                if (span <= 0.0) return gainsDb[i + 1]
                val t = ((x - xs[i]) / span).toFloat()
                return gainsDb[i] + t * (gainsDb[i + 1] - gainsDb[i])
            }
        }
        return 0f
    }

    /** بازه مجاز اسلایدر کاربر — سقف‌گذاری‌شده برای جلوگیری از تقویت افراطی */
    fun uiRange(minDb: Float, maxDb: Float): ClosedFloatingPointRange<Float> {
        val bottom = maxOf(minDb, -MAX_USER_BOOST_DB)
        val top = minOf(maxDb, MAX_USER_BOOST_DB)
        return bottom..top
    }

    /** دسی‌بل → میلی‌بِل (واحد API افکت‌های اندروید) با محدودسازی به بازه دستگاه */
    fun dbToMillibels(db: Float, minDb: Float, maxDb: Float): Int =
        (db.coerceIn(minDb, maxDb) * 100).roundToInt()
}
