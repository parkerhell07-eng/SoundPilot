package com.soundpilot.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerPresetsTest {

    @Test
    fun `car preset compensates road noise with boosted lows and highs`() {
        val centers = listOf(60, 230, 960, 3600, 12_000)
        val gains = EqualizerMath.gainsFor(
            EqualizerPresets.CAR,
            centers,
            minDb = -15f,
            maxDb = 15f,
        )
        assertEquals(centers.size, gains.size)
        // فرکانس پایین (مقابل صدای موتور) باید تقویت محسوسی داشته باشد
        assertTrue("باند ۶۰Hz در حالت خودرو باید بیش از +۳dB باشد: ${gains.first()}", gains.first() > 3f)
        // فرکانس بالا (وضوح در برابر سروصدا) هم تقویت شود
        assertTrue("باند ۱۲kHz در حالت خودرو باید تقویت داشته باشد: ${gains.last()}", gains.last() > 1f)
        // و همه در سقف امن پروفایل‌ها باشند
        gains.forEach { assertTrue(it <= EqualizerMath.MAX_PRESET_BOOST_DB + 0.001f) }
    }

    @Test
    fun `all ten presets are registered with unique ids`() {
        assertEquals(10, EqualizerPresets.all.size)
        val ids = EqualizerPresets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        listOf("balanced", "car", "bass", "vocal", "clarity", "rock", "pop", "jazz", "classical", "electronic")
            .forEach { id ->
                assertTrue("پیش‌فرض $id باید ثبت شده باشد", ids.contains(id))
            }
    }

    @Test
    fun `byId falls back to balanced for unknown id`() {
        assertEquals(EqualizerPresets.BALANCED.id, EqualizerPresets.byId("unknown").id)
        assertEquals(EqualizerPresets.BALANCED.id, EqualizerPresets.byId("").id)
    }

    @Test
    fun `every preset curve samples safely on typical device bands`() {
        // باندهای رایج اکولایزرهای ۵ و ۱۰ باندی سamsung/سایر دستگاه‌ها
        val centers5 = listOf(60, 230, 960, 3600, 12_000)
        val centers10 = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16_000)
        EqualizerPresets.all.forEach { preset ->
            listOf(centers5, centers10).forEach { centers ->
                val gains = EqualizerMath.gainsFor(preset, centers, -15f, 15f)
                assertEquals("تعداد بهره‌ها برای ${preset.id} باید برابر باند باشد", centers.size, gains.size)
                gains.forEach { gain ->
                    assertTrue("بهره ${preset.id} خارج بازه امن: $gain", gain in -15f..15f)
                }
            }
        }
    }
}
