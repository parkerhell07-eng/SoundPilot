package com.soundpilot.app.data

import com.soundpilot.app.di.ServiceLocator
import com.soundpilot.app.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface LibraryState {
    data object Loading : LibraryState
    data object Empty : LibraryState
    data class Ready(val songs: List<Song>) : LibraryState
    data class Error(val message: String) : LibraryState
}

/**
 * حافظه موقت سطح برنامه برای کتابخانه موسیقی.
 * یک‌بار از MediaStore خوانده و بین صفحه‌ها و سرویس پخش به اشتراک گذاشته می‌شود.
 */
object LibraryStore {

    private val _state = MutableStateFlow<LibraryState>(LibraryState.Loading)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private var songsById: Map<Long, Song> = emptyMap()
    private var loadedOnce = false

    fun songById(id: Long): Song? = songsById[id]

    suspend fun ensureLoaded() {
        if (!loadedOnce || _state.value is LibraryState.Error) refresh()
    }

    suspend fun refresh() {
        _state.value = LibraryState.Loading
        try {
            val songs = ServiceLocator.musicRepository.loadSongs()
            songsById = songs.associateBy { it.id }
            loadedOnce = true
            _state.value = if (songs.isEmpty()) LibraryState.Empty else LibraryState.Ready(songs)
        } catch (_: SecurityException) {
            loadedOnce = false
            _state.value = LibraryState.Error(
                "دسترسی به کتابخانه موسیقی رد شد. لطفاً مجوز را در تنظیمات برنامه فعال کنید.",
            )
        } catch (e: Exception) {
            loadedOnce = false
            _state.value = LibraryState.Error(
                "خواندن کتابخانه موسیقی ناموفق بود${e.message?.let { ": $it" } ?: "."}",
            )
        }
    }
}
