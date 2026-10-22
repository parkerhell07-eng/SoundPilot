package com.soundpilot.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCompatTest {

    // ---------------- parseVersion ----------------

    @Test
    fun `parseVersion با پیشوند v`() {
        assertEquals(listOf(1, 4, 0), UpdateCompat.parseVersion("v1.4.0"))
        assertEquals(listOf(1, 4, 0), UpdateCompat.parseVersion("V1.4.0"))
    }

    @Test
    fun `parseVersion دو بخشی و پسوند`() {
        assertEquals(listOf(1, 4), UpdateCompat.parseVersion("1.4"))
        assertEquals(listOf(1, 4, 0), UpdateCompat.parseVersion("1.4.0-beta"))
        assertEquals(listOf(2, 0, 1), UpdateCompat.parseVersion("2.0.1+build5"))
    }

    @Test
    fun `parseVersion نامعتبر null می‌دهد`() {
        assertNull(UpdateCompat.parseVersion(null))
        assertNull(UpdateCompat.parseVersion(""))
        assertNull(UpdateCompat.parseVersion("نسخه-جدید"))
        assertNull(UpdateCompat.parseVersion("abc"))
    }

    // ---------------- isNewer ----------------

    @Test
    fun `نسخه جدیدتر تشخیص داده می‌شود`() {
        assertTrue(UpdateCompat.isNewer("1.4.0", "1.3.0"))
        assertTrue(UpdateCompat.isNewer("v1.10.0", "v1.9.9"))
        assertTrue(UpdateCompat.isNewer("2.0", "1.99.99"))
        assertTrue(UpdateCompat.isNewer("1.3.1", "1.3.0"))
    }

    @Test
    fun `نسخه برابر یا قدیمی‌تر بروزرسانی حساب نمی‌شود`() {
        assertFalse(UpdateCompat.isNewer("1.3.0", "1.3.0"))
        assertFalse(UpdateCompat.isNewer("1.2.9", "1.3.0"))
        assertFalse(UpdateCompat.isNewer(null, "1.3.0"))
        assertFalse(UpdateCompat.isNewer("نسخه", "1.3.0"))
    }

    @Test
    fun `نسخه فعلی نامعتبر یعنی هر نسخه ریموت معتبر جدیدتر است`() {
        assertTrue(UpdateCompat.isNewer("0.0.1", null))
        assertTrue(UpdateCompat.isNewer("0.0.1", ""))
    }

    // ---------------- parseRelease ----------------

    private fun releaseJson(
        tag: String = "v1.4.0",
        draft: Boolean = false,
        vararg assets: Triple<String, Long, String>,
    ): String {
        val assetJson = assets.joinToString(",") { (name, size, url) ->
            """{"name":"$name","size":$size,"browser_download_url":"$url","content_type":"application/octet-stream"}"""
        }
        return """{
            "url":"https://api.github.com/repos/x/y/releases/1",
            "tag_name":"$tag",
            "name":"نسخه $tag",
            "draft":$draft,
            "prerelease":false,
            "published_at":"2026-10-22T10:00:00Z",
            "body":"## تازه‌ها\n- **افکت صوتی جدید**\n- رفع باگ `پخش`",
            "assets":[${assetJson}]
        }"""
    }

    @Test
    fun `پارس پاسخ استاندارد گیت‌هاب و انتخاب APK release`() {
        val info = UpdateCompat.parseRelease(
            releaseJson(
                "v1.4.0",
                false,
                Triple("SoundPilot-v1.4.0-debug.apk", 100L, "https://x/debug.apk"),
                Triple("SoundPilot-v1.4.0-release.apk", 2_600_000L, "https://x/release.apk"),
                Triple("src.zip", 132L, "https://x/src.zip"),
            ),
        )
        assertNotNull(info)
        assertEquals("v1.4.0", info!!.tagName)
        assertEquals("1.4.0", info.version)
        assertEquals("SoundPilot-v1.4.0-release.apk", info.apkName)
        assertEquals(2_600_000L, info.apkSizeBytes)
        assertEquals("https://x/release.apk", info.apkUrl)
        assertTrue(info.notes.contains("افکت صوتی جدید"))
        // پاک‌سازی markdown
        assertFalse(info.notes.contains("**"))
        assertFalse(info.notes.contains("##"))
        assertFalse(info.notes.contains("`"))
    }

    @Test
    fun `release بدون APK release از APK غیر debug استفاده می‌کند`() {
        val info = UpdateCompat.parseRelease(
            releaseJson(
                "v1.4.0",
                false,
                Triple("app-other.apk", 10L, "https://x/other.apk"),
            ),
        )
        assertNotNull(info)
        assertEquals("app-other.apk", info!!.apkName)
    }

    @Test
    fun `release بدون APK مناسب یا draft پذیرفته نمی‌شود`() {
        assertNull(UpdateCompat.parseRelease(releaseJson())) // بدون asset
        assertNull(
            UpdateCompat.parseRelease(
                releaseJson(
                    "v1.4.0",
                    true,
                    Triple("SoundPilot-v1.4.0-release.apk", 1L, "https://x/r.apk"),
                ),
            ),
        )
    }

    @Test
    fun `پاسخ خراب گیت‌هاب باعث کرش نمی‌شود`() {
        assertNull(UpdateCompat.parseRelease("not json at all"))
        assertNull(UpdateCompat.parseRelease("{"))
        assertNull(UpdateCompat.parseRelease("[]"))
    }

    // ---------------- cleanNotes ----------------

    @Test
    fun `پاک‌سازی یادداشت انتشار`() {
        val raw = "## تازه‌ها\n\n- **قابلیت یک**\n- قابلیت [دو](https://example.com)\n\n\n\nپایان"
        val cleaned = UpdateCompat.cleanNotes(raw)
        assertFalse(cleaned.contains("##"))
        assertFalse(cleaned.contains("**"))
        assertFalse(cleaned.contains("(https://example.com)"))
        assertTrue(cleaned.contains("قابلیت دو"))
        assertFalse(cleaned.contains("\n\n\n"))
    }

    @Test
    fun `یادداشت خالی رشته خالی می‌دهد`() {
        assertEquals("", UpdateCompat.cleanNotes(null))
        assertEquals("", UpdateCompat.cleanNotes("   "))
    }

    // ---------------- formatBytes ----------------

    @Test
    fun `نمایش حجم فایل`() {
        assertEquals("0 KB", UpdateCompat.formatBytes(0))
        assertEquals("1 KB", UpdateCompat.formatBytes(1024))
        assertEquals("2.5 MB", UpdateCompat.formatBytes((2.5 * 1024 * 1024).toLong()))
        assertEquals("2 MB", UpdateCompat.formatBytes(2 * 1024 * 1024))
    }
}
