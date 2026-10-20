package com.soundpilot.app.data

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.soundpilot.app.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * خواندن موسیقی محلی از MediaStore.
 * - فقط فایل‌های صوتی با مدت ≥ ۳۰ ثانیه (حذف رینگتون و کلیپ‌های کوتاه)
 * - هیچ داده‌ای از دستگاه خارج نمی‌شود.
 */
class MediaStoreMusicRepository(private val context: Context) {

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
        )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val result = ArrayList<Song>(256)
        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                if (id <= 0) continue
                val trackRaw = c.getString(6)
                result += Song(
                    id = id,
                    title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "بدون عنوان",
                    artist = c.getString(2)?.takeIf { it.isNotBlank() } ?: "هنرمند ناشناس",
                    album = c.getString(3)?.takeIf { it.isNotBlank() } ?: "آلبوم ناشناس",
                    albumId = c.getLong(4),
                    durationMs = c.getLong(5),
                    trackNumber = trackRaw?.substringBefore('/')?.toIntOrNull() ?: 0,
                    year = c.getInt(7),
                    dateAddedSec = c.getLong(8),
                    sizeBytes = c.getLong(9),
                    mimeType = c.getString(10) ?: "",
                )
            }
        }
        result
    }
}
