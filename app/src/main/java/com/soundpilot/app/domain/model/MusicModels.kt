package com.soundpilot.app.domain.model

/**
 * مدل اصلی آهنگ — از MediaStore خوانده می‌شود.
 * داده‌ها فقط در دستگاه باقی می‌مانند (حریم خصوصی).
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
    val dateAddedSec: Long,
    val sizeBytes: Long,
    val mimeType: String,
) {
    val contentUri: String
        get() = "content://media/external/audio/media/$id"
}

data class Album(
    val albumId: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val totalDurationMs: Long,
    /** آهنگ شاخص آلبوم برای واکشی کاور از MediaStore */
    val firstSongId: Long = 0,
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
)

enum class SortMode(val id: String) {
    TITLE("title"),
    ARTIST("artist"),
    ALBUM("album"),
    DURATION("duration"),
    RECENT("recent"),
    ;

    companion object {
        fun fromId(id: String?): SortMode = entries.firstOrNull { it.id == id } ?: TITLE
    }
}

enum class LibraryTab {
    SONGS,
    ALBUMS,
    ARTISTS,
    FAVORITES,
}
