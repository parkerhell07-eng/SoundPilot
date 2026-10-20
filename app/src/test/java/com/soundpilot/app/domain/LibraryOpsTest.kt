package com.soundpilot.app.domain

import com.soundpilot.app.domain.model.Song
import com.soundpilot.app.domain.model.SortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryOpsTest {

    private fun song(
        id: Long,
        title: String,
        artist: String,
        album: String,
        albumId: Long = 1,
        durationMs: Long = 60_000,
        track: Int = 1,
        dateAdded: Long = 0,
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = durationMs,
        trackNumber = track,
        year = 0,
        dateAddedSec = dateAdded,
        sizeBytes = 0,
        mimeType = "audio/mpeg",
    )

    private val library = listOf(
        song(1, "دیلاور", "محسن چاوشی", "سلام", albumId = 10, track = 2, dateAdded = 100),
        song(2, "باران", "محسن چاوشی", "سلام", albumId = 10, track = 1, dateAdded = 200),
        song(3, "Delbar", "همایون شجریان", "راز", albumId = 20, track = 1, dateAdded = 300),
        song(4, "یاس", "Tataloo", "یاس", albumId = 30, track = 1, dateAdded = 400),
    )

    @Test
    fun `filter matches title artist and album with persian normalization`() {
        // ی عربی باید با ی فارسی مطابقت کند
        assertEquals(1, LibraryOps.filterSongs(library, "ياس").size)
        // حروف بزرگ/کوچک لاتین
        assertEquals(1, LibraryOps.filterSongs(library, "DELBAR").size)
        // جست‌وجو در نام خواننده
        assertEquals(2, LibraryOps.filterSongs(library, "چاوشی").size)
        // جست‌وجو در نام آلبوم
        assertEquals(2, LibraryOps.filterSongs(library, "سلام").size)
        // بدون عبارت → همه
        assertEquals(library.size, LibraryOps.filterSongs(library, "").size)
    }

    @Test
    fun `sort by title uses normalized comparison`() {
        val sorted = LibraryOps.sortSongs(library, SortMode.TITLE)
        assertEquals("Delbar", sorted[0].title)
        assertEquals("باران", sorted[1].title)
        assertEquals("دیلاور", sorted[2].title)
        assertEquals("یاس", sorted[3].title)
    }

    @Test
    fun `sort by recent puts newest first`() {
        val sorted = LibraryOps.sortSongs(library, SortMode.RECENT)
        assertEquals(4L, sorted.first().id)
        assertEquals(1L, sorted.last().id)
    }

    @Test
    fun `albums group by albumId and sum duration`() {
        val albums = LibraryOps.albums(library)
        assertEquals(3, albums.size)
        val salam = albums.first { it.albumId == 10L }
        assertEquals("سلام", salam.name)
        assertEquals(2, salam.songCount)
        assertEquals(120_000L, salam.totalDurationMs)
        assertEquals("محسن چاوشی", salam.artist)
    }

    @Test
    fun `artists count songs and distinct albums`() {
        val artists = LibraryOps.artists(library)
        assertEquals(3, artists.size)
        val chavoshi = artists.first { it.name == "محسن چاوشی" }
        assertEquals(2, chavoshi.songCount)
        assertEquals(1, chavoshi.albumCount)
    }

    @Test
    fun `songsOfAlbum sorts by track number`() {
        val songs = LibraryOps.songsOfAlbum(library, 10L)
        assertEquals(listOf(2L, 1L), songs.map { it.id })
    }

    @Test
    fun `songsOfArtist filters by artist only`() {
        val songs = LibraryOps.songsOfArtist(library, "محسن چاوشی")
        assertEquals(2, songs.size)
        assertTrue(songs.all { it.artist == "محسن چاوشی" })
    }

    // ---------- ایندکس جست‌وجوی آماده (مسیر جدید نسخه ۱.۱.۰) ----------

    @Test
    fun `filterPrepared matches filterSongs results`() {
        val prepared = LibraryOps.prepare(library)
        assertEquals(
            LibraryOps.filterSongs(library, "چاوشی").map { it.id },
            LibraryOps.filterPrepared(prepared, "چاوشی").map { it.id },
        )
        assertEquals(
            LibraryOps.filterSongs(library, "ياس").map { it.id },
            LibraryOps.filterPrepared(prepared, "ياس").map { it.id },
        )
        assertEquals(
            LibraryOps.filterSongs(library, "DELBAR").map { it.id },
            LibraryOps.filterPrepared(prepared, "DELBAR").map { it.id },
        )
    }

    @Test
    fun `filterPrepared with empty query returns all songs`() {
        val prepared = LibraryOps.prepare(library)
        assertEquals(library.size, LibraryOps.filterPrepared(prepared, "").size)
        assertEquals(library.size, LibraryOps.filterPrepared(prepared, "   ").size)
    }

    @Test
    fun `sortPrepared matches sortSongs ordering`() {
        val prepared = LibraryOps.prepare(library)
        assertEquals(
            LibraryOps.sortSongs(library, SortMode.TITLE).map { it.id },
            LibraryOps.sortPrepared(prepared, SortMode.TITLE).map { it.id },
        )
        assertEquals(
            LibraryOps.sortSongs(library, SortMode.RECENT).map { it.id },
            LibraryOps.sortPrepared(prepared, SortMode.RECENT).map { it.id },
        )
    }

    @Test
    fun `albums include representative song id for artwork`() {
        val albums = LibraryOps.albums(library)
        val salam = albums.first { it.albumId == 10L }
        // یکی از آهنگ‌های واقعی آلبوم برای واکشی کاور
        assertTrue(salam.firstSongId == 1L || salam.firstSongId == 2L)
    }
}
