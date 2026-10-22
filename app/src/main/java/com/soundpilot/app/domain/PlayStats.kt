package com.soundpilot.app.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * آمار پخش هر آهنگ — فقط روی خود دستگاه ذخیره می‌شود.
 * count = چند بار شروع به پخش شده؛ lastPlayedAt = آخرین زمان شروع پخش (epoch ms).
 */
@Serializable
data class PlayStatEntry(
    val count: Int = 0,
    val lastPlayedAt: Long = 0,
)

/**
 * منطق خالص آمار پخش — بدون وابستگی به Android و کاملاً قابل تست واحد.
 */
object PlayStatsOps {

    private val json = Json { ignoreUnknownKeys = true }

    /** به‌روزرسانی آمار برای یک پخش جدید: شمارش +۱ و زمان به‌روز */
    fun bump(
        stats: Map<Long, PlayStatEntry>,
        songId: Long,
        now: Long,
        cap: Int = 300,
    ): Map<Long, PlayStatEntry> {
        if (songId <= 0) return stats
        val current = stats[songId] ?: PlayStatEntry()
        val updated = stats + (
            songId to PlayStatEntry(
                count = (current.count + 1).coerceAtMost(Int.MAX_VALUE),
                lastPlayedAt = now,
            )
            )
        return prune(updated, cap)
    }

    /**
     * نگه‌داشتن حداکثر cap رکورد — قدیمی‌ترین‌ها (بر اساس آخرین پخش) حذف می‌شوند
     * تا JSON ذخیره‌سازی همیشه کوچک و سریع بماند.
     */
    fun prune(stats: Map<Long, PlayStatEntry>, cap: Int): Map<Long, PlayStatEntry> {
        if (stats.size <= cap) return stats
        return stats.entries
            .sortedByDescending { it.value.lastPlayedAt }
            .take(cap)
            .associate { it.toPair() }
    }

    /**
     * شناسه‌های «پخش‌های اخیر» به ترتیب تازگی — فقط آهنگ‌هایی که واقعاً پخش شده‌اند.
     */
    fun recentSongIds(stats: Map<Long, PlayStatEntry>, limit: Int = 12): List<Long> =
        stats.asSequence()
            .filter { (id, entry) -> id > 0 && entry.lastPlayedAt > 0 && entry.count > 0 }
            .sortedByDescending { it.value.lastPlayedAt }
            .take(limit.coerceAtLeast(0))
            .map { it.key }
            .toList()

    // ---------- سریال‌سازی (سازگاری با داده‌های قدیمی) ----------

    fun encode(stats: Map<Long, PlayStatEntry>): String = json.encodeToString(stats)

    fun decode(raw: String?): Map<Long, PlayStatEntry> =
        raw?.let {
            runCatching {
                json.decodeFromString<Map<Long, PlayStatEntry>>(it)
            }.getOrNull()
        } ?: emptyMap()
}
