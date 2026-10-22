package com.soundpilot.app.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * اطلاعات نسخه جدید — استخراج‌شده از GitHub Releases.
 * فقط فیلدهای لازم برای جریان «بررسی → دانلود → نصب».
 */
data class ReleaseInfo(
    val tagName: String,
    /** نسخه بدون پیشوند v — مثل 1.4.0 */
    val version: String,
    val title: String,
    /** متن یادداشت انتشار، پاک‌سازی‌شده برای نمایش ساده */
    val notes: String,
    val apkName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val publishedAtIso: String,
)

@Serializable
internal data class GhAssetDto(
    val name: String = "",
    val size: Long = 0,
    @SerialName("browser_download_url") val url: String = "",
)

@Serializable
internal data class GhReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GhAssetDto> = emptyList(),
)

/**
 * منطق خالص بروزرسانی — بدون وابستگی به Android و کاملاً قابل تست واحد.
 */
object UpdateCompat {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * تبدیل برچسب نسخه به اعداد: "v1.4.0" → [1,4,0]، "1.4" → [1,4].
     * هر چیزی بعد از رقم‌های هر بخش نادیده گرفته می‌شود ("1.4.0-beta" → [1,4,0]).
     * نسخه نامعتبر → null.
     */
    fun parseVersion(raw: String?): List<Int>? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().removePrefix("v").removePrefix("V")
        val parts = cleaned.split('.', '-', '_', '+')
            .mapNotNull { seg -> seg.takeWhile { it.isDigit() }.takeIf { it.isNotEmpty() }?.toIntOrNull() }
        return if (parts.isEmpty() || parts.size > 4) null else parts
    }

    /** آیا نسخه ریموت جدیدتر از نسخه فعلی است؟ */
    fun isNewer(remote: String?, current: String?): Boolean {
        val r = parseVersion(remote) ?: return false
        val c = parseVersion(current) ?: return true
        val n = maxOf(r.size, c.size)
        for (i in 0 until n) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    /** انتخاب فایل APK درست: اولویت با نسخه release؛ نسخه debug هرگز انتخاب نمی‌شود */
    internal fun pickApkAsset(assets: List<GhAssetDto>): GhAssetDto? =
        assets.firstOrNull { it.name.endsWith("-release.apk") }
            ?: assets.firstOrNull { it.name.endsWith(".apk") && !it.name.contains("debug", ignoreCase = true) }

    /**
     * تحلیل پاسخ releases/latest گیت‌هاب.
     * اگر draft باشد یا APK مناسب نداشته باشد → null.
     */
    fun parseRelease(body: String): ReleaseInfo? = runCatching {
        val dto = json.decodeFromString<GhReleaseDto>(body)
        if (dto.draft || dto.tagName.isBlank()) return@runCatching null
        val asset = pickApkAsset(dto.assets) ?: return@runCatching null
        if (asset.url.isBlank()) return@runCatching null
        val version = parseVersion(dto.tagName)?.joinToString(".") ?: dto.tagName.removePrefix("v")
        ReleaseInfo(
            tagName = dto.tagName,
            version = version,
            title = dto.name?.takeIf { it.isNotBlank() } ?: dto.tagName,
            notes = cleanNotes(dto.body),
            apkName = asset.name,
            apkUrl = asset.url,
            apkSizeBytes = asset.size,
            publishedAtIso = dto.publishedAt.orEmpty(),
        )
    }.getOrNull()

    /**
     * پاک‌سازی متن Markdown یادداشت انتشار برای نمایش ساده در اپ:
     * علامت‌های سرتیتر، ستاره‌های بولد و لینک‌ها ساده می‌شوند؛ خط‌های خالی تکراری جمع می‌شوند.
     */
    fun cleanNotes(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val lines = raw.lines()
            .map { line ->
                var l = line.trimEnd()
                l = l.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                l = l.replace(Regex("\\*(.+?)\\*"), "$1")
                l = l.replace(Regex("`{1,3}([^`]*)`{1,3}"), "$1")
                l = l.replace(Regex("\\[([^]]+)]\\(([^)]*)\\)"), "$1")
                l = l.replace(Regex("^#{1,6}\\s*"), "")
                l
            }
            .map { if (it.isBlank()) "" else it }
        val sb = StringBuilder()
        var blankRun = 0
        for (line in lines) {
            if (line.isEmpty()) {
                blankRun++
                if (blankRun <= 1) sb.append('\n')
            } else {
                blankRun = 0
                sb.append(line).append('\n')
            }
        }
        return sb.toString().trim()
    }

    /** نمایش خوانا برای حجم فایل — ارقام لاتین؛ فارسی‌سازی در لایه UI انجام می‌شود */
    fun formatBytes(bytes: Long): String = when {
        bytes <= 0 -> "0 KB"
        bytes < 1024 * 1024 -> "${(bytes + 1023) / 1024} KB"
        else -> {
            val mb = bytes / (1024f * 1024f)
            val rounded = (mb * 10).toInt() / 10f
            if (rounded % 1f == 0f) "${rounded.toInt()} MB" else "$rounded MB"
        }
    }
}
