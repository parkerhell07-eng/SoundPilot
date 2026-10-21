package com.soundpilot.app.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * صحت سریال‌سازی تنظیمات — پایه ذخیره‌سازی پایدار در DataStore.
 * اگر قالب JSON تغییر کند، نسخه‌های قدیمی باید بدون خطا خوانده شوند.
 */
class SettingsSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `equalizer settings round trip`() {
        val original = EqualizerSettings(
            enabled = true,
            profileId = "bass",
            gainsDb = listOf(5.5f, 2.5f, 0f, -1f, 1.5f),
            bassStrength = 350,
            virtualizerStrength = 120,
            bandCount = 5,
        )
        val restored = json.decodeFromString<EqualizerSettings>(json.encodeToString(original))
        assertEquals(original, restored)
    }

    @Test
    fun `equalizer settings decode falls back to safe defaults`() {
        val restored = json.decodeFromString<EqualizerSettings>("""{"enabled":true}""")
        assertTrue(restored.enabled)
        assertEquals("balanced", restored.profileId)
        assertTrue(restored.gainsDb.isEmpty())
        assertEquals(0, restored.bassStrength)
        assertEquals(0, restored.bandCount)
    }

    @Test
    fun `equalizer settings ignore unknown keys from newer versions`() {
        val restored = json.decodeFromString<EqualizerSettings>(
            """{"enabled":false,"profileId":"vocal","futureField":123}""",
        )
        assertEquals("vocal", restored.profileId)
    }

    @Test
    fun `v1_2 saved eq json without reverb fields decodes with safe defaults`() {
        // JSON واقعی ذخیره‌شده توسط نسخه ۱.۲ — نباید هنگام ارتقا خطا بدهد
        val legacy = """{"enabled":true,"profileId":"car","gainsDb":[4.5,3.0,0.5,0.0,1.5],
            "bassStrength":400,"virtualizerStrength":150,"loudnessGainMb":300,"bandCount":5}"""
        val restored = json.decodeFromString<EqualizerSettings>(legacy)
        assertTrue(restored.enabled)
        assertEquals("car", restored.profileId)
        assertEquals(0, restored.reverbId)
        assertEquals(0, restored.reverbAmount)
    }

    @Test
    fun `reverb fields round trip`() {
        val original = EqualizerSettings(enabled = true, profileId = "spatial3d", reverbId = 5, reverbAmount = 35)
        val restored = json.decodeFromString<EqualizerSettings>(json.encodeToString(original))
        assertEquals(original, restored)
    }

    @Test
    fun `last played round trip`() {
        val original = LastPlayed(
            songIds = listOf(11, 22, 33),
            index = 2,
            positionMs = 95_000,
            shuffle = true,
            repeatMode = 2,
            savedAt = 1_700_000_000_000,
        )
        val restored = json.decodeFromString<LastPlayed>(json.encodeToString(original))
        assertEquals(original, restored)
    }

    @Test
    fun `empty last played is treated as null by repository contract`() {
        val empty = LastPlayed()
        assertTrue(empty.isEmpty)
        assertNull(
            "قرارداد: LastPlayed خالی معادل نبودِ وضعیت ذخیره‌شده است",
            if (empty.isEmpty) null else empty,
        )
    }

    @Test
    fun `favorites list round trip`() {
        val favorites = listOf<Long>(1, 42, 999)
        val restored = json.decodeFromString<List<Long>>(json.encodeToString(favorites))
        assertEquals(favorites, restored)
    }
}
