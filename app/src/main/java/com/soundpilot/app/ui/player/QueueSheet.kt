package com.soundpilot.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soundpilot.app.ui.components.SongArtwork
import com.soundpilot.app.util.PersianText

/** ورق پایینی صف پخش: پرش به آیتم و حذف آیتم */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<PlaybackViewModel.QueueEntry>,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
        ) {
            Text(
                text = "صف پخش (${PersianText.toPersianDigits(queue.size.toString())} آهنگ)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (queue.isEmpty()) {
                Text(
                    text = "صف پخش خالی است.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                    itemsIndexed(queue, key = { _, entry -> "${entry.index}-${entry.mediaId ?: -1}" }) { _, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onJump(entry.index) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.width(28.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (entry.isCurrent) {
                                    Icon(
                                        Icons.Rounded.GraphicEq,
                                        contentDescription = "در حال پخش",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Text(
                                        text = PersianText.toPersianDigits((entry.index + 1).toString()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            SongArtwork(
                                albumId = entry.albumId,
                                songId = entry.mediaId ?: 0L,
                                size = 42.dp,
                                cornerRadius = 8.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (entry.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (entry.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (entry.artist.isNotBlank()) {
                                    Text(
                                        text = entry.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            IconButton(onClick = { onRemove(entry.index) }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "حذف ${entry.title} از صف پخش",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
