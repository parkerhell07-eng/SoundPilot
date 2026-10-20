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

    /** نسخه نرمال‌شده‌شده آهنگ برای جست‌وجو/مرتب‌سازی سریع (یک‌بار محاسبه، هزاران بار استفاده) */
    data class PreparedSong(
        val song: Song,
        val nTitle: String,
        val nArtist: String,
        val nAlbum: String,
        val searchText: String,
    )

    /**
     * ایندکس جست‌وجو را یک‌بار برای کل کتابخانه می‌سازد؛
     * این کار نرمال‌سازی regex روی هر کلید تایپ را حذف می‌کند (بهبود چشمگیر سرعت).
     */
    fun prepare(songs: List<Song>): List<PreparedSong> = songs.map { s ->
        val t = PersianText.normalize(s.title)
        val a = PersianText.normalize(s.artist)
        val al = PersianText.normalize(s.album)
        PreparedSong(
            song = s,
            nTitle = t,
            nArtist = a,
            nAlbum = al,
            searchText = "$t $a $al",
        )
    }

    fun filterSongs(songs: List<Song>, rawQuery: String): List<Song> {
        val q = PersianText.normalize(rawQuery)
        if (q.isEmpty()) return songs
        return songs.filter { s ->
            PersianText.normalize(s.title).contains(q) ||
                PersianText.normalize(s.artist).contains(q) ||
                PersianText.normalize(s.album).contains(q)
        }
    }

    /** فیلتر روی ایندکس آماده — بدون هیچ نرمال‌سازی تکراری */
    fun filterPrepared(prepared: List<PreparedSong>, rawQuery: String): List<Song> {
        val q = PersianText.normalize(rawQuery)
        if (q.isEmpty()) return prepared.map { it.song }
        return prepared
            .filter { it.searchText.contains(q) }
            .map { it.song }
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

    /** مرتب‌سازی روی نسخه آماده — کلیدهای نرمال‌شده از قبل محاسبه شده‌اند */
    fun sortPrepared(prepared: List<PreparedSong>, mode: SortMode): List<Song> = when (mode) {
        SortMode.TITLE -> prepared.sortedWith(
            compareBy({ it.nTitle }, { it.nArtist }),
        ).map { it.song }
        SortMode.ARTIST -> prepared.sortedWith(
            compareBy({ it.nArtist }, { it.nAlbum }, { it.song.trackNumber }),
        ).map { it.song }
        SortMode.ALBUM -> prepared.sortedWith(
            compareBy({ it.nAlbum }, { it.song.trackNumber }),
        ).map { it.song }
        SortMode.DURATION -> prepared.sortedByDescending { it.song.durationMs }.map { it.song }
        SortMode.RECENT -> prepared.sortedByDescending { it.song.dateAddedSec }.map { it.song }
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
                firstSongId = first.id,
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
