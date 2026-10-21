package com.soundpilot.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * صحت ۲۰ پریست صوتی استودیو — قلب قابلیت صوتی نسخه ۱.۳.
 * هر پریست باید شناسه یکتا، مقادیر امن و منحنی قابل نمونه‌برداری روی هر دستگاهی داشته باشد.
 */
class SoundPresetsTest {

    @Test
    fun `exactly twenty presets registered with unique ids and non-empty labels`() {
        assertEquals(20, SoundPresets.all.size)
        val ids = SoundPresets.all.map { it.id }
        assertEquals("شناسه‌ها باید یکتا باشند", ids.size, ids.toSet().size)
        ids.forEach { id ->
            assertTrue("شناسه نباید خالی باشد", id.isNotBlank())
        }
        SoundPresets.all.forEach { preset ->
            assertTrue("برچسب ${preset.id} نباید خالی باشد", preset.label.isNotBlank())
            assertTrue("توضیح ${preset.id} نباید خالی باشد", preset.subtitle.isNotBlank())
        }
    }

    @Test
    fun `every preset keeps effect values in safe ranges`() {
        SoundPresets.all.forEach { preset ->
            assertTrue("بیس ${preset.id} خارج بازه: ${preset.bass}", preset.bass in 0..1000)
            assertTrue("فراگیر ${preset.id} خارج بازه: ${preset.surround}", preset.surround in 0..1000)
            assertTrue(
                "بلندی ${preset.id} خارج بازه: ${preset.loudnessMb}",
                preset.loudnessMb in 0..AudioEffectsController.MAX_LOUDNESS_GAIN_MB,
            )
            assertTrue(
                "میزان طنین ${preset.id} خارج بازه: ${preset.reverbAmount}",
                preset.reverbAmount in 0..100,
            )
            if (preset.reverb == ReverbPreset.OFF) {
                assertEquals("طنین خاموش باید میزان صفر داشته باشد: ${preset.id}", 0, preset.reverbAmount)
            } else {
                assertTrue("طنین فعال باید میزان مثبت داشته باشد: ${preset.id}", preset.reverbAmount > 0)
            }
        }
    }

    @Test
    fun `every preset curve samples safely on typical device bands`() {
        val centers5 = listOf(60, 230, 960, 3600, 12_000)
        val centers10 = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16_000)
        SoundPresets.all.forEach { preset ->
            listOf(centers5, centers10).forEach { centers ->
                val gains = EqualizerMath.gainsFor(
                    EqCurve(preset.id, preset.label, preset.curve),
                    centers,
                    -15f,
                    15f,
                )
                assertEquals(
                    "تعداد بهره‌ها برای ${preset.id} باید برابر باند باشد",
                    centers.size,
                    gains.size,
                )
                gains.forEach { gain ->
                    assertTrue("بهره ${preset.id} خارج بازه امن: $gain", gain in -15f..15f)
                    assertTrue(
                        "سقف تقویت پریست‌ها برای جلوگیری از اعوجاج: ${preset.id}",
                        gain <= EqualizerMath.MAX_PRESET_BOOST_DB + 0.001f,
                    )
                }
            }
        }
    }

    @Test
    fun `weak-system presets compensate with loudness or bass`() {
        // سیستم‌های ضعیف به جبران نویز/کمبود بیس نیاز دارند
        assertTrue(SoundPresets.CAR_ECONOMY.loudnessMb > 0 || SoundPresets.CAR_ECONOMY.bass > 0)
        assertEquals(SoundTier.WEAK, SoundPresets.CAR_ECONOMY.tier)
        assertEquals(SoundTier.STRONG, SoundPresets.CAR_PRO.tier)
    }

    @Test
    fun `spatial preset pushes surround while late night pushes loudness`() {
        assertTrue("پریست سه‌بعدی باید فراگیر بالا داشته باشد", SoundPresets.SPATIAL_3D.surround >= 800)
        assertTrue("پریست شبانه باید بلندی بالا داشته باشد", SoundPresets.LATE_NIGHT.loudnessMb >= 800)
        assertEquals(ReverbPreset.OFF, SoundPresets.LATE_NIGHT.reverb)
    }

    @Test
    fun `byId resolves all ids and maps legacy presets for backward compatibility`() {
        SoundPresets.all.forEach { preset ->
            assertEquals(preset.id, SoundPresets.byId(preset.id).id)
        }
        // نگاشت نسخه‌های قبل
        assertEquals(SoundPresets.FLAT.id, SoundPresets.byId("balanced").id)
        assertEquals(SoundPresets.CAR_STANDARD.id, SoundPresets.byId("car").id)
        assertEquals(SoundPresets.BASS_CLEAN.id, SoundPresets.byId("bass").id)
        assertEquals(SoundPresets.CRYSTAL.id, SoundPresets.byId("clarity").id)
        assertEquals(SoundPresets.EDM.id, SoundPresets.byId("electronic").id)
        // ناشناخته‌ها به مرجع خنثی برمی‌گردند
        assertEquals(SoundPresets.FLAT.id, SoundPresets.byId("unknown").id)
        assertEquals(SoundPresets.FLAT.id, SoundPresets.byId(null).id)
    }
}

class ReverbPresetTest {

    @Test
    fun `seven presets with hardware ids and unique labels`() {
        assertEquals(7, ReverbPreset.entries.size)
        val ids = ReverbPreset.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        // شناسه‌های سخت‌افزاری اندروید: ۰ تا ۶
        assertEquals((0..6).toList(), ids.sorted())
        ReverbPreset.entries.forEach { assertTrue("برچسب ${it.name} نباید خالی باشد", it.label.isNotBlank()) }
    }

    @Test
    fun `fromId falls back to OFF for unknown ids`() {
        assertEquals(ReverbPreset.OFF, ReverbPreset.fromId(-1))
        assertEquals(ReverbPreset.OFF, ReverbPreset.fromId(99))
        assertEquals(ReverbPreset.PLATE, ReverbPreset.fromId(6))
    }
}

class AudioDeviceCategoryTest {

    @Test
    fun `bluetooth device types map to bluetooth category`() {
        assertEquals(AudioDeviceCategory.BLUETOOTH, AudioDeviceCategory.fromDeviceType(8)) // A2DP
        assertEquals(AudioDeviceCategory.BLUETOOTH, AudioDeviceCategory.fromDeviceType(26)) // BLE
    }

    @Test
    fun `wired and usb map correctly`() {
        assertEquals(AudioDeviceCategory.WIRED, AudioDeviceCategory.fromDeviceType(4)) // HEADPHONES
        assertEquals(AudioDeviceCategory.WIRED, AudioDeviceCategory.fromDeviceType(3)) // HEADSET
        assertEquals(AudioDeviceCategory.USB, AudioDeviceCategory.fromDeviceType(11))
        assertEquals(AudioDeviceCategory.USB, AudioDeviceCategory.fromDeviceType(13)) // USB_HEADSET
    }

    @Test
    fun `builtin speaker and unknown types fall back safely`() {
        assertEquals(AudioDeviceCategory.SPEAKER, AudioDeviceCategory.fromDeviceType(2))
        assertEquals(AudioDeviceCategory.OTHER, AudioDeviceCategory.fromDeviceType(-12345))
    }
}
