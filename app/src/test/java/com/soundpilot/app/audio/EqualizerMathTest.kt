package com.soundpilot.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerMathTest {

    private val line10dB = listOf(100f to 0f, 10_000f to 10f)

    @Test
    fun `sampleCurve interpolates linearly in log-frequency domain`() {
        // نقطه میانی لگاریتمی بین ۱۰۰ و ۱۰ هزار، دقیقاً ۱۰۰۰ هرتز است
        val gain = EqualizerMath.sampleCurve(line10dB, 1000f)
        assertEquals(5f, gain, 0.01f)
    }

    @Test
    fun `sampleCurve clamps outside curve range`() {
        assertEquals(0f, EqualizerMath.sampleCurve(line10dB, 50f), 0.001f)
        assertEquals(10f, EqualizerMath.sampleCurve(line10dB, 20_000f), 0.001f)
    }

    @Test
    fun `sampleCurve returns zero for empty curve`() {
        assertEquals(0f, EqualizerMath.sampleCurve(emptyList(), 1000f), 0.001f)
    }

    @Test
    fun `gainsFor produces one gain per band and respects preset boost cap`() {
        val centers = listOf(60, 230, 960, 3600, 12_000)
        val gains = EqualizerMath.gainsFor(
            EqualizerPresets.BASS,
            centers,
            minDb = -15f,
            maxDb = 15f,
        )
        assertEquals(centers.size, gains.size)
        gains.forEach { gain ->
            assertTrue("هر بهره باید در سقف ۶+ دسی‌بل باشد: $gain", gain <= EqualizerMath.MAX_PRESET_BOOST_DB + 0.001f)
        }
        // فرکانس پایین پروفایل بیس باید بیشترین تقویت را داشته باشد
        assertTrue(gains.first() > 4.5f)
    }

    @Test
    fun `gainsFor clamps to narrow hardware range`() {
        val centers = listOf(60, 230, 960, 3600, 12_000)
        val gains = EqualizerMath.gainsFor(
            EqualizerPresets.BASS,
            centers,
            minDb = -5f,
            maxDb = 5f,
        )
        gains.forEach { gain ->
            assertTrue(gain in -5f..5f)
        }
    }

    @Test
    fun `uiRange limits user boost even when hardware allows more`() {
        val range = EqualizerMath.uiRange(-15f, 15f)
        assertEquals(-EqualizerMath.MAX_USER_BOOST_DB, range.start, 0.001f)
        assertEquals(EqualizerMath.MAX_USER_BOOST_DB, range.endInclusive, 0.001f)
    }

    @Test
    fun `dbToMillibels converts and clamps`() {
        assertEquals(150, EqualizerMath.dbToMillibels(1.5f, -15f, 15f))
        assertEquals(1500, EqualizerMath.dbToMillibels(20f, -15f, 15f))
        assertEquals(-1500, EqualizerMath.dbToMillibels(-20f, -15f, 15f))
    }

    @Test
    fun `every preset has unique id and non empty curve`() {
        val ids = EqualizerPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        EqualizerPresets.all.forEach { preset ->
            assertTrue(preset.points.isNotEmpty())
            assertTrue(preset.label.isNotBlank())
        }
    }
}
