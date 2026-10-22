package com.soundpilot.app.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soundpilot.app.domain.model.Album
import com.soundpilot.app.domain.model.Artist
import com.soundpilot.app.domain.model.LibraryTab
import com.soundpilot.app.domain.model.Song
import com.soundpilot.app.domain.model.SortMode
import com.soundpilot.app.ui.components.EmptyState
import com.soundpilot.app.ui.components.SongArtworkFill
import com.soundpilot.app.ui.components.SongRow
import com.soundpilot.app.ui.player.PlaybackViewModel
import com.soundpilot.app.ui.theme.SpColors
import com.soundpilot.app.util.PersianText

@Composable
fun LibraryScreen(
    playbackVm: PlaybackViewModel,
    onOpenPlayer: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val vm: LibraryViewModel = viewModel()
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val currentSongId by playbackVm.currentSongId.collectAsStateWithLifecycle()
    val isPlaying by playbackVm.isPlaying.collectAsStateWithLifecycle()

    var searchActive by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // هدر
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "SoundPilot",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        brush = Brush.horizontalGradient(listOf(SpColors.Amber, SpColors.Teal)),
                        fontWeight = FontWeight.Black,
                    ),
                )
                Text(
                    text = "موزیک همراه جاده",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpColors.Amber,
                )
            }
            IconButton(onClick = {
                searchActive = !searchActive
                if (!searchActive) vm.setQuery("")
            }) {
                Icon(Icons.Rounded.Search, contentDescription = "جست‌وجو")
            }
            Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "مرتب‌سازی")
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false },
                ) {
                    SortMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(mode)) },
                            onClick = {
                                vm.setSortMode(mode)
                                sortMenuOpen = false
                            },
                            trailingIcon = {
                                if (ui.sortMode == mode) {
                                    Icon(Icons.Rounded.Check, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }
            IconButton(onClick = vm::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی کتابخانه")
            }
        }

        AnimatedVisibility(visible = searchActive) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("جست‌وجوی آهنگ، خواننده یا آلبوم…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (ui.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "پاک‌کردن جست‌وجو")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        Spacer(Modifier.height(4.dp))

        TabRow(selectedTabIndex = ui.tab.ordinal) {
            LibraryTab.entries.forEach { tab ->
                Tab(
                    selected = ui.tab == tab,
                    onClick = { vm.setTab(tab) },
                    text = { Text(tabLabel(tab)) },
                )
            }
        }

        when {
            ui.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            ui.error != null -> {
                EmptyState(
                    icon = Icons.Rounded.ErrorOutline,
                    title = "خطا در بارگذاری کتابخانه",
                    subtitle = ui.error,
                    actionLabel = "تلاش دوباره",
                    onAction = vm::refresh,
                )
            }
            ui.empty -> {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "هیچ موسیقی پیدا نشد",
                    subtitle = "آهنگ‌هایی به گوشی خود اضافه کنید و دوباره تلاش کنید.",
                    actionLabel = "به‌روزرسانی",
                    onAction = vm::refresh,
                )
            }
            else -> {
                Crossfade(targetState = ui.tab, label = "libraryTabs") { tab ->
                    when (tab) {
                        LibraryTab.SONGS -> SongsList(
                            songs = ui.songs,
                            favorites = ui.favorites,
                            currentSongId = currentSongId,
                            isPlaying = isPlaying,
                            playbackVm = playbackVm,
                            showNoResults = ui.query.isNotBlank() && ui.songs.isEmpty(),
                            recentSongs = ui.recentSongs,
                            showRecent = ui.query.isBlank(),
                        )
                        LibraryTab.ALBUMS -> AlbumsGrid(
                            albums = ui.albums,
                            onOpenAlbum = onOpenAlbum,
                            showNoResults = ui.query.isNotBlank() && ui.albums.isEmpty(),
                        )
                        LibraryTab.ARTISTS -> ArtistsList(
                            artists = ui.artists,
                            onOpenArtist = onOpenArtist,
                            showNoResults = ui.query.isNotBlank() && ui.artists.isEmpty(),
                        )
                        LibraryTab.FAVORITES -> {
                            if (ui.favoriteSongs.isEmpty()) {
                                EmptyState(
                                    icon = Icons.Rounded.FavoriteBorder,
                                    title = "هنوز علاقه‌مندی ندارید",
                                    subtitle = "با لمس آیکون قلب کنار هر آهنگ، آن را اینجا ببینید.",
                                )
                            } else {
                                SongsList(
                                    songs = ui.favoriteSongs,
                                    favorites = ui.favorites,
                                    currentSongId = currentSongId,
                                    isPlaying = isPlaying,
                                    playbackVm = playbackVm,
                                    showNoResults = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongsList(
    songs: List<Song>,
    favorites: Set<Long>,
    currentSongId: Long?,
    isPlaying: Boolean,
    playbackVm: PlaybackViewModel,
    showNoResults: Boolean,
    recentSongs: List<Song> = emptyList(),
    showRecent: Boolean = false,
) {
    if (showNoResults) {
        EmptyState(
            icon = Icons.Rounded.Search,
            title = "نتیجه‌ای پیدا نشد",
            subtitle = "عبارت دیگری را امتحان کنید.",
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (showRecent && recentSongs.isNotEmpty()) {
            item(key = "recent") {
                RecentSection(
                    songs = recentSongs,
                    currentSongId = currentSongId,
                    onPlay = { index -> playbackVm.play(recentSongs, index) },
                )
            }
        }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                song = song,
                isFavorite = song.id in favorites,
                isCurrent = song.id == currentSongId,
                isPlaying = isPlaying,
                onClick = { playbackVm.play(songs, index) },
                onToggleFavorite = { playbackVm.toggleFavorite(song.id) },
                onPlayNext = { playbackVm.playNext(song) },
                onAddToQueue = { playbackVm.addToQueue(song) },
            )
        }
    }
}

@Composable
private fun AlbumsGrid(
    albums: List<Album>,
    onOpenAlbum: (Long) -> Unit,
    showNoResults: Boolean,
) {
    if (showNoResults) {
        EmptyState(
            icon = Icons.Rounded.Search,
            title = "نتیجه‌ای پیدا نشد",
            subtitle = "عبارت دیگری را امتحان کنید.",
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(albums, key = { it.albumId }) { album ->
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAlbum(album.albumId) },
                ) {
                    SongArtworkFill(
                        albumId = album.albumId,
                        songId = album.firstSongId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${PersianText.toPersianDigits(album.songCount.toString())} آهنگ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistsList(
    artists: List<Artist>,
    onOpenArtist: (String) -> Unit,
    showNoResults: Boolean,
) {
    if (showNoResults) {
        EmptyState(
            icon = Icons.Rounded.Search,
            title = "نتیجه‌ای پیدا نشد",
            subtitle = "عبارت دیگری را امتحان کنید.",
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(artists, key = { it.name }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onOpenArtist(artist.name) }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${PersianText.toPersianDigits(artist.songCount.toString())} آهنگ • " +
                            "${PersianText.toPersianDigits(artist.albumCount.toString())} آلبوم",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun tabLabel(tab: LibraryTab): String = when (tab) {
    LibraryTab.SONGS -> "آهنگ‌ها"
    LibraryTab.ALBUMS -> "آلبوم‌ها"
    LibraryTab.ARTISTS -> "خواننده‌ها"
    LibraryTab.FAVORITES -> "علاقه‌مندی‌ها"
}

private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.TITLE -> "عنوان"
    SortMode.ARTIST -> "خواننده"
    SortMode.ALBUM -> "آلبوم"
    SortMode.DURATION -> "مدت"
    SortMode.RECENT -> "جدیدترین"
}

/** ردیف افقی «پخش‌های اخیر» — بر اساس آمار واقعی پخش روی خود دستگاه */
@Composable
private fun RecentSection(
    songs: List<Song>,
    currentSongId: Long?,
    onPlay: (Int) -> Unit,
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.History,
                contentDescription = null,
                tint = SpColors.Amber,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "پخش‌های اخیر",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(songs, key = { _, song -> "recent-${song.id}" }) { index, song ->
                Column(
                    modifier = Modifier
                        .width(104.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onPlay(index) }
                        .padding(4.dp),
                ) {
                    Box {
                        SongArtworkFill(
                            albumId = song.albumId,
                            songId = song.id,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                        if (song.id == currentSongId) {
                            Surface(
                                color = SpColors.Amber,
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.GraphicEq,
                                    contentDescription = "در حال پخش",
                                    tint = androidx.compose.ui.graphics.Color.Black,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(12.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
