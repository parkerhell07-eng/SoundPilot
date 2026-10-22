package com.soundpilot.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayStatsTest {

    // ---------------- bump ----------------

    @Test
    fun `پخش جدید شمارش و زمان را به‌روز می‌کند`() {
        val stats = PlayStatsOps.bump(emptyMap(), songId = 10, now = 1_000L)
        assertEquals(PlayStatEntry(count = 1, lastPlayedAt = 1_000L), stats[10])
        val again = PlayStatsOps.bump(stats, songId = 10, now = 2_000L)
        assertEquals(PlayStatEntry(count = 2, lastPlayedAt = 2_000L), again[10])
    }

    @Test
    fun `شناسه نامعتبر نادیده گرفته می‌شود`() {
        val stats = PlayStatsOps.bump(emptyMap(), songId = 0, now = 1_000L)
        assertTrue(stats.isEmpty())
        assertTrue(PlayStatsOps.bump(emptyMap(), songId = -5, now = 1_000L).isEmpty())
    }

    @Test
    fun `حافظه محدود — قدیمی‌ترین‌ها حذف می‌شوند`() {
        var stats = emptyMap<Long, PlayStatEntry>()
        // ۵۰ آهنگ با زمان‌های صعودی
        for (i in 1..50) {
            stats = PlayStatsOps.bump(stats, i.toLong(), now = i * 1_000L, cap = 10)
        }
        assertEquals(10, stats.size)
        // ۱۰ تازه‌ترین (۴۱ تا ۵۰) باقی مانده‌اند
        assertEquals(50, stats.keys.max())
        assertEquals(41, stats.keys.min())
        assertFalse(stats.containsKey(1L))
    }

    // ---------------- recentSongIds ----------------

    @Test
    fun `ترتیب پخش‌های اخیر بر اساس تازگی است`() {
        val stats = mapOf(
            1L to PlayStatEntry(3, 100L),
            2L to PlayStatEntry(1, 300L),
            3L to PlayStatEntry(5, 200L),
        )
        assertEquals(listOf(2L, 3L, 1L), PlayStatsOps.recentSongIds(stats, limit = 12))
    }

    @Test
    fun `آمار بدون زمان یا شمارش صفر نادیده گرفته می‌شود`() {
        val stats = mapOf(
            1L to PlayStatEntry(0, 500L),
            2L to PlayStatEntry(2, 0L),
            3L to PlayStatEntry(1, 400L),
        )
        assertEquals(listOf(3L), PlayStatsOps.recentSongIds(stats, limit = 12))
    }

    @Test
    fun `محدودیت تعداد رعایت می‌شود`() {
        var stats = emptyMap<Long, PlayStatEntry>()
        for (i in 1..30) stats = PlayStatsOps.bump(stats, i.toLong(), now = i * 100L)
        val recent = PlayStatsOps.recentSongIds(stats, limit = 12)
        assertEquals(12, recent.size)
        assertEquals(30L, recent.first())
        assertEquals(19L, recent.last())
    }

    // ---------------- encode/decode (سازگاری) ----------------

    @Test
    fun `حلقه encode و decode بی‌اتلاف است`() {
        val stats = mapOf(
            1L to PlayStatEntry(2, 1_000L),
            9L to PlayStatEntry(7, 9_000L),
        )
        val decoded = PlayStatsOps.decode(PlayStatsOps.encode(stats))
        assertEquals(stats, decoded)
    }

    @Test
    fun `داده خراب یا خالی به نقشه خالی برمی‌گردد`() {
        assertTrue(PlayStatsOps.decode(null).isEmpty())
        assertTrue(PlayStatsOps.decode("").isEmpty())
        assertTrue(PlayStatsOps.decode("{invalid json").isEmpty())
    }

    @Test
    fun `فیلد ناشناخته در JSON مشکلی ایجاد نمی‌کند`() {
        val decoded = PlayStatsOps.decode("""{"1":{"count":3,"lastPlayedAt":500,"future":true}}""")
        assertEquals(PlayStatEntry(3, 500), decoded[1L])
    }
}
