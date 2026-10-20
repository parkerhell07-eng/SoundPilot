package com.soundpilot.app.domain

import com.soundpilot.app.domain.model.Album
import com.soundpilot.app.domain.model.Artist
import com.soundpilot.app.domain.model.Song
import com.soundpilot.app.domain.model.SortMode
import com.soundpilot.app.util.PersianText

/**
 * منطق خالص کتابخانه (فیلتر، مرتب‌سازی، گروه‌بندی) — بدون وابستگی به Android
 * و کاملاً قابل تست واحد.
 */
object LibraryOps {

    fun filterSongs(songs: List<Song>, rawQuery: String): List<Song> {
        val q = PersianText.normalize(rawQuery)
        if (q.isEmpty()) return songs
        return songs.filter { s ->
            PersianText.normalize(s.title).contains(q) ||
                PersianText.normalize(s.artist).contains(q) ||
                PersianText.normalize(s.album).contains(q)
        }
    }

    fun sortSongs(songs: List<Song>, mode: SortMode): List<Song> = when (mode) {
        SortMode.TITLE -> songs.sortedWith(
            compareBy({ PersianText.normalize(it.title) }, { PersianText.normalize(it.artist) }),
        )
        SortMode.ARTIST -> songs.sortedWith(
            compareBy(
                { PersianText.normalize(it.artist) },
                { PersianText.normalize(it.album) },
                { it.trackNumber },
            ),
        )
        SortMode.ALBUM -> songs.sortedWith(
            compareBy({ PersianText.normalize(it.album) }, { it.trackNumber }),
        )
        SortMode.DURATION -> songs.sortedByDescending { it.durationMs }
        SortMode.RECENT -> songs.sortedByDescending { it.dateAddedSec }
    }

    fun albums(songs: List<Song>): List<Album> = songs
        .groupBy { it.albumId }
        .map { (_, group) ->
            val first = group.first()
            Album(
                albumId = first.albumId,
                name = first.album,
                artist = first.artist,
                songCount = group.size,
                totalDurationMs = group.sumOf { it.durationMs },
            )
        }
        .sortedBy { PersianText.normalize(it.name) }

    fun artists(songs: List<Song>): List<Artist> = songs
        .groupBy { PersianText.normalize(it.artist) }
        .map { (_, group) ->
            Artist(
                name = group.first().artist,
                songCount = group.size,
                albumCount = group.map { it.albumId }.distinct().size,
            )
        }
        .sortedBy { PersianText.normalize(it.name) }

    fun songsOfAlbum(songs: List<Song>, albumId: Long): List<Song> =
        songs
            .filter { it.albumId == albumId }
            .sortedWith(compareBy({ it.trackNumber }, { PersianText.normalize(it.title) }))

    fun songsOfArtist(songs: List<Song>, artistName: String): List<Song> =
        songs
            .filter { it.artist == artistName }
            .sortedWith(compareBy({ PersianText.normalize(it.album) }, { it.trackNumber }))
}
