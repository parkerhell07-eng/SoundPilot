package com.soundpilot.app.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.soundpilot.app.data.AlbumArtRepository
import com.soundpilot.app.domain.model.Song

/** کلیدهای اضافی داخل MediaMetadata برای انتقال شناسه‌ها به سرویس/صف */
const val EXTRA_ALBUM_ID = "com.soundpilot.app.ALBUM_ID"

/**
 * تبدیل مدل دامنه به MediaItem استاندارد Media3 با متادیتای کامل.
 *
 * [artworkUri] بهتر است از AlbumArtRepository بیاید (URI قدیمی آلبوم‌آرت
 * در اندروید ۱۰+ غیرقابل‌اعتماد است)؛ اگر null باشد، کش موجود استفاده می‌شود.
 */
fun Song.toMediaItem(artworkUri: String? = null): MediaItem {
    val resolvedArtwork = artworkUri ?: AlbumArtRepository.cachedUri(albumId)
    val extras = Bundle().apply { putLong(EXTRA_ALBUM_ID, albumId) }
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Uri.parse(contentUri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(resolvedArtwork?.let { Uri.parse(it) } ?: Uri.EMPTY)
                .setExtras(extras)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()
}
