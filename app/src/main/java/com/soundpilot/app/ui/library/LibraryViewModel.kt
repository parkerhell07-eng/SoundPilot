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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

/** نتیجه محاسبات سنگین کتابخانه که یک‌جا و روی ترد پس‌زمینه انجام می‌شود */
private data class LibraryComputed(
    val storeState: LibraryState,
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
)

/**
 * ViewModel کتابخانه با سه بهینه‌سازی کلیدی برای سرعت:
 * ۱) ایندکس جست‌وجوی نرمال‌شده که یک‌بار برای کل کتابخانه ساخته می‌شود
 *    (به‌جای نرمال‌سازی regex همه فیلدها روی هر حرف تایپ).
 * ۲) فیلتر با debounce روی ورودی جست‌وجو؛ TextField فوری می‌ماند.
 * ۳) همه محاسبات فیلتر/گروه‌بندی روی Dispatchers.Default — ترد UI هرگز بلاک نمی‌شود.
 */
@OptIn(FlowPreview::class)
class LibraryViewModel : ViewModel() {

    private val settings = ServiceLocator.settingsRepository
    private val queryInputFlow = MutableStateFlow("")
    private val tabFlow = MutableStateFlow(LibraryTab.SONGS)

    /** ورودی فوری برای نمایش در TextField */
    val queryInput: StateFlow<String> = queryInputFlow

    /** محاسبات سنگین: ایندکس + فیلتر debounced + گروه‌بندی — همه روی ترد پس‌زمینه */
    private val computed: StateFlow<LibraryComputed> = combine(
        LibraryStore.state,
        queryInputFlow.debounce(180),
    ) { storeState, query ->
        val songs = (storeState as? LibraryState.Ready)?.songs ?: emptyList()
        val index = LibraryOps.prepare(songs)
        val filtered = LibraryOps.filterPrepared(index, query)
        LibraryComputed(
            storeState = storeState,
            songs = filtered,
            albums = LibraryOps.albums(filtered),
            artists = LibraryOps.artists(filtered),
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryComputed(LibraryState.Loading, emptyList(), emptyList(), emptyList()))

    val uiState: StateFlow<LibraryUiState> = combine(
        computed,
        queryInputFlow,
        tabFlow,
        settings.sortMode,
        settings.favorites,
    ) { data, query, tab, sort, favorites ->
        val sorted = LibraryOps.sortSongs(data.songs, sort)
        LibraryUiState(
            tab = tab,
            query = query,
            sortMode = sort,
            songs = sorted,
            albums = data.albums,
            artists = data.artists,
            favorites = favorites,
            favoriteSongs = sorted.filter { it.id in favorites },
            loading = data.storeState is LibraryState.Loading,
            error = (data.storeState as? LibraryState.Error)?.message,
            empty = data.storeState is LibraryState.Empty,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(query: String) {
        queryInputFlow.value = query
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
