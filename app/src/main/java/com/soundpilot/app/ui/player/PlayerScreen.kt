package com.soundpilot.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundpilot.app.data.SettingsRepository
import com.soundpilot.app.di.ServiceLocator
import com.soundpilot.app.ui.components.ArtworkFill
import com.soundpilot.app.util.PersianText
import kotlinx.coroutines.launch

/**
 * صفحه پخش کامل با دو چیدمان:
 * ۱) چیدمان کامل (آرت، اسلایدر، کنترل‌ها)
 * ۲) حالت خودرو: دکمه‌های بزرگ و حداقل تعامل برای استفاده ایمن حین رانندگی
 */
@Composable
fun PlayerScreen(
    playbackVm: PlaybackViewModel,
    onBack: () -> Unit,
) {
    val isPlaying by playbackVm.isPlaying.collectAsStateWithLifecycle()
    val metadata by playbackVm.currentMetadata.collectAsStateWithLifecycle()
    val positionMs by playbackVm.positionMs.collectAsStateWithLifecycle()
    val durationMs by playbackVm.durationMs.collectAsStateWithLifecycle()
    val shuffleEnabled by playbackVm.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by playbackVm.repeatMode.collectAsStateWithLifecycle()
    val currentSongId by playbackVm.currentSongId.collectAsStateWithLifecycle()
    val favorites by playbackVm.favorites.collectAsStateWithLifecycle()
    val outputDevice by playbackVm.outputDeviceName.collectAsStateWithLifecycle()

    val settings = ServiceLocator.settingsRepository
    val carMode by settings.carMode.collectAsStateWithLifecycle(false)
    val queue by playbackVm.queue.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    var showQueue by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        playbackVm.errors.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            // نوار بالا
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "بستن صفحه پخش",
                    )
                }
                Spacer(Modifier.weight(1f))
                if (outputDevice.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = "خروجی: $outputDevice",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    scope.launch { settings.setCarMode(!carMode) }
                }) {
                    Icon(
                        Icons.Rounded.DirectionsCar,
                        contentDescription = if (carMode) "خروج از حالت خودرو" else "ورود به حالت خودرو",
                        tint = if (carMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val title = metadata?.title?.toString() ?: "آهنگی انتخاب نشده"
            val artist = metadata?.artist?.toString() ?: ""

            if (carMode) {
                Spacer(Modifier.weight(1f))
                CarControls(
                    title = title,
                    artist = artist,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onTogglePlayPause = playbackVm::togglePlayPause,
                    onNext = playbackVm::next,
                    onPrevious = playbackVm::previous,
                )
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.height(8.dp))
                ArtworkFill(
                    uri = metadata?.artworkUri?.toString(),
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (artist.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(16.dp))
                SeekRow(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeek = playbackVm::seekTo,
                )
                Spacer(Modifier.height(8.dp))
                FullControls(
                    isPlaying = isPlaying,
                    shuffleEnabled = shuffleEnabled,
                    repeatMode = repeatMode,
                    isFavorite = currentSongId != null && currentSongId in favorites,
                    onTogglePlayPause = playbackVm::togglePlayPause,
                    onNext = playbackVm::next,
                    onPrevious = playbackVm::previous,
                    onToggleShuffle = playbackVm::toggleShuffle,
                    onCycleRepeat = playbackVm::cycleRepeatMode,
                    onToggleFavorite = { playbackVm.toggleFavorite(currentSongId) },
                    onOpenQueue = { showQueue = true },
                )
                Spacer(Modifier.weight(1f))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }

    if (showQueue) {
        QueueSheet(
            queue = queue,
            onDismiss = { showQueue = false },
            onJump = playbackVm::jumpTo,
            onRemove = playbackVm::removeFromQueue,
        )
    }
}

/** نوار پیشرفت با زمان — همیشه چپ‌به‌راست برای خوانایی خط زمان */
@Composable
private fun SeekRow(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val displayPosition = dragFraction?.let { (it * durationMs).toLong() } ?: positionMs

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr,
    ) {
        Column {
            androidx.compose.material3.Slider(
                value = if (durationMs > 0) (displayPosition.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let { fraction -> onSeek((fraction * durationMs).toLong()) }
                    dragFraction = null
                },
                enabled = durationMs > 0,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = PersianText.formatDuration(displayPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = PersianText.formatDuration(durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FullControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    val repeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = if (shuffleEnabled) "خروج از پخش تصادفی" else "پخش تصادفی",
                    tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = "آهنگ قبلی",
                    modifier = Modifier.size(44.dp),
                )
            }
            FilledIconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(84.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "توقف" else "پخش",
                    modifier = Modifier.size(46.dp),
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = "آهنگ بعدی",
                    modifier = Modifier.size(44.dp),
                )
            }
            IconButton(onClick = onCycleRepeat) {
                val icon = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) {
                    Icons.Rounded.RepeatOne
                } else {
                    Icons.Rounded.Repeat
                }
                Icon(
                    imageVector = icon,
                    contentDescription = repeatLabel(repeatMode),
                    tint = if (repeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenQueue) {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = "نمایش صف پخش",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** چیدمان ایمن خودرو: دکمه‌های خیلی بزرگ، بدون تعامل پرت‌وجه */
@Composable
private fun CarControls(
    title: String,
    artist: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "حالت خودرو فعال است",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "برای ایمنی، توجه خود را به جاده بدهید",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (artist.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(26.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(96.dp),
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "آهنگ قبلی",
                        modifier = Modifier.size(48.dp),
                    )
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(128.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "توقف" else "پخش",
                        modifier = Modifier.size(68.dp),
                    )
                }
                FilledIconButton(
                    onClick = onNext,
                    modifier = Modifier.size(96.dp),
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "آهنگ بعدی",
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = {
                    if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${PersianText.formatDuration(positionMs)} از ${PersianText.formatDuration(durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun repeatLabel(mode: Int): String = when (mode) {
    androidx.media3.common.Player.REPEAT_MODE_ALL -> "تکرار همه"
    androidx.media3.common.Player.REPEAT_MODE_ONE -> "تکرار همین آهنگ"
    else -> "بدون تکرار"
}
