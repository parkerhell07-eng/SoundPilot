package com.soundpilot.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundpilot.app.data.LibraryState
import com.soundpilot.app.data.LibraryStore
import com.soundpilot.app.domain.LibraryOps
import com.soundpilot.app.ui.components.Artwork
import com.soundpilot.app.ui.components.EmptyState
import com.soundpilot.app.ui.components.SongRow
import com.soundpilot.app.ui.player.PlaybackViewModel
import kotlin.random.Random

/**
 * صفحه جزئیات آلبوم یا خواننده — همان ردیف آهنگ با هدر اختصاصی.
 */
@Composable
fun CollectionDetailScreen(
    albumId: Long? = null,
    artistName: String? = null,
    playbackVm: PlaybackViewModel,
    onBack: () -> Unit,
) {
    val storeState by LibraryStore.state.collectAsStateWithLifecycle()
    val favorites by playbackVm.favorites.collectAsStateWithLifecycle()
    val currentSongId by playbackVm.currentSongId.collectAsStateWithLifecycle()

    val songs = remember(storeState, albumId, artistName) {
        val all = (storeState as? LibraryState.Ready)?.songs ?: emptyList()
        when {
            albumId != null -> LibraryOps.songsOfAlbum(all, albumId)
            artistName != null -> LibraryOps.songsOfArtist(all, artistName)
            else -> emptyList()
        }
    }

    val title = when {
        albumId != null -> songs.firstOrNull()?.album ?: "آلبوم"
        artistName != null -> artistName ?: "خواننده"
        else -> ""
    }
    val subtitle = when {
        albumId != null -> songs.firstOrNull()?.artist ?: ""
        artistName != null ->
            "${PersianTextCompat.songsLabel(songs.size)} • " +
                PersianTextCompat.albumsLabel(songs.map { it.albumId }.distinct().size)
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = if (albumId != null) "آلبوم" else "خواننده",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        when {
            storeState is LibraryState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            songs.isEmpty() -> {
                EmptyState(
                    icon = Icons.Rounded.Shuffle,
                    title = "آیتمی پیدا نشد",
                    subtitle = "ممکن است محتوا تغییر کرده باشد.",
                    actionLabel = "بازگشت",
                    onAction = onBack,
                )
            }
            else -> {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Artwork(
                        uri = songs.firstOrNull()?.albumArtUri,
                        size = 168.dp,
                        cornerRadius = 20.dp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { playbackVm.play(songs, 0) }) {
                            Text("پخش همه")
                        }
                        FilledTonalButton(onClick = {
                            val start = if (songs.size > 1) Random.nextInt(songs.size) else 0
                            playbackVm.play(songs, start)
                        }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("تصادفی")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        SongRow(
                            song = song,
                            isFavorite = song.id in favorites,
                            isCurrent = song.id == currentSongId,
                            onClick = { playbackVm.play(songs, index) },
                            onToggleFavorite = { playbackVm.toggleFavorite(song.id) },
                            onPlayNext = { playbackVm.playNext(song) },
                            onAddToQueue = { playbackVm.addToQueue(song) },
                        )
                    }
                }
            }
        }
    }
}

/** کمک‌خوب برای برچسب‌های فارسی در این فایل */
private object PersianTextCompat {
    fun songsLabel(count: Int): String =
        "${com.soundpilot.app.util.PersianText.toPersianDigits(count.toString())} آهنگ"

    fun albumsLabel(count: Int): String =
        "${com.soundpilot.app.util.PersianText.toPersianDigits(count.toString())} آلبوم"
}
