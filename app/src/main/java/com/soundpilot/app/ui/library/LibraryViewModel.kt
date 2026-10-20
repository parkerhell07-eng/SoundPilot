package com.soundpilot.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soundpilot.app.data.LibraryState
import com.soundpilot.app.data.LibraryStore
import com.soundpilot.app.di.ServiceLocator
import com.soundpilot.app.domain.LibraryOps
import com.soundpilot.app.domain.model.Album
import com.soundpilot.app.domain.model.Artist
import com.soundpilot.app.domain.model.LibraryTab
import com.soundpilot.app.domain.model.Song
import com.soundpilot.app.domain.model.SortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.SONGS,
    val query: String = "",
    val sortMode: SortMode = SortMode.TITLE,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val favoriteSongs: List<Song> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val empty: Boolean = false,
)

class LibraryViewModel : ViewModel() {

    private val settings = ServiceLocator.settingsRepository
    private val queryFlow = MutableStateFlow("")
    private val tabFlow = MutableStateFlow(LibraryTab.SONGS)

    val uiState: StateFlow<LibraryUiState> = combine(
        LibraryStore.state,
        queryFlow,
        tabFlow,
        settings.sortMode,
        settings.favorites,
    ) { storeState, query, tab, sort, favorites ->
        val songs = (storeState as? LibraryState.Ready)?.songs ?: emptyList()
        val filtered = LibraryOps.sortSongs(LibraryOps.filterSongs(songs, query), sort)
        LibraryUiState(
            tab = tab,
            query = query,
            sortMode = sort,
            songs = filtered,
            albums = LibraryOps.albums(filtered),
            artists = LibraryOps.artists(filtered),
            favorites = favorites,
            favoriteSongs = filtered.filter { it.id in favorites },
            loading = storeState is LibraryState.Loading,
            error = (storeState as? LibraryState.Error)?.message,
            empty = storeState is LibraryState.Empty,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    fun setTab(tab: LibraryTab) {
        tabFlow.value = tab
    }

    fun setSortMode(mode: SortMode) {
        viewModelScope.launch { settings.setSortMode(mode) }
    }

    fun refresh() {
        viewModelScope.launch { LibraryStore.refresh() }
    }
}
