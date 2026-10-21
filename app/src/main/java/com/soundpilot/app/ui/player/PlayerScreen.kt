package com.soundpilot.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundpilot.app.data.SettingsRepository
import com.soundpilot.app.di.ServiceLocator
import com.soundpilot.app.ui.components.BlurredArtBackdrop
import com.soundpilot.app.ui.components.GlassSurface
import com.soundpilot.app.ui.components.PlayingBarsIndicator
import com.soundpilot.app.ui.components.SongArtworkFill
import com.soundpilot.app.ui.theme.SpColors
import com.soundpilot.app.util.PersianText
import kotlinx.coroutines.launch

/**
 * صفحه پخش کامل با دو چیدمان:
 * ۱) چیدمان کامل (آرت، اسلایدر، کنترل‌ها)
 * ۲) حالت خودرو: دکمه‌های بزرگ و حداقل تعامل برای استفاده ایمن حین رانندگی
 *
 * نکته: کنترل‌های پخش طبق استاندارد جهانی همیشه چپ‌به‌راست چیده می‌شوند
 * (قبلی در چپ، بعدی در راست) — مستقل از راست‌چین‌بودن رابط فارسی.
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
    val currentSong by playbackVm.currentSong.collectAsStateWithLifecycle()
    val paletteColor by playbackVm.playerPalette.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    var showQueue by remember { mutableStateOf(false) }

    // رنگ لهجه داینامیک از کاور — با انتقال نرم
    val accent = if (paletteColor != null) Color(paletteColor!!) else SpColors.Amber

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // کشیدن افقی روی صفحه پخش = تغییر آهنگ (امکان استفاده بدون نگاه به صفحه)
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        when {
                            totalDrag < -SWIPE_THRESHOLD_PX -> playbackVm.next()
                            totalDrag > SWIPE_THRESHOLD_PX -> playbackVm.previous()
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                }
            },
    ) {
        // پس‌زمینه: کاور بلورشده آهنگ جاری، در نبود کاور، شفق رنگی
        if (currentSong != null) {
            BlurredArtBackdrop(
                albumId = currentSong?.albumId ?: 0L,
                songId = currentSong?.id ?: 0L,
                scrimColor = SpColors.NightBase,
            )
        } else {
            com.soundpilot.app.ui.components.AuroraBackdrop()
        }

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
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, "بستن صفحه پخش", onClick = onBack)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "در حال پخش",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (outputDevice.isNotBlank()) {
                        Text(
                            text = outputDevice,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                GlassIconButton(
                    icon = Icons.Rounded.DirectionsCar,
                    contentDescription = if (carMode) "خروج از حالت خودرو" else "ورود به حالت خودرو",
                    tint = if (carMode) SpColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { scope.launch { settings.setCarMode(!carMode) } },
                )
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
                AlbumArtStage(
                    albumId = currentSong?.albumId ?: 0L,
                    songId = currentSong?.id ?: 0L,
                    isPlaying = isPlaying,
                    accent = accent,
                    hasCurrent = currentSong != null,
                )
                Spacer(Modifier.height(20.dp))

                // تعویض نرم عنوان/خواننده با تغییر آهنگ
                AnimatedContent(
                    targetState = currentSongId,
                    transitionSpec = {
                        (slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(320)))
                            .togetherWith(slideOutVertically(tween(220)) { -it / 3 } + fadeOut(tween(180)))
                    },
                    label = "songMeta",
                ) { _ ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
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
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (isPlaying) {
                    PlayingBarsIndicator(
                        isPlaying = true,
                        color = accent,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                } else {
                    Spacer(Modifier.height(18.dp))
                }

                Spacer(Modifier.height(8.dp))
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

/** دکمه شیشه‌ای کوچک نوار بالا */
@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .background(SpColors.GlassFill),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = contentDescription, tint = tint)
        }
    }
}

/** صحنه آرت آلبوم: قاب شیشه‌ای، درخشش رنگی و تنفس ملایم حین پخش */
@Composable
private fun AlbumArtStage(
    albumId: Long,
    songId: Long,
    isPlaying: Boolean,
    accent: Color,
    hasCurrent: Boolean,
) {
    val breathing = rememberInfiniteTransition(label = "breath")
    val scale by breathing.animateFloat(
        initialValue = 1f,
        targetValue = 1.022f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "breathScale",
    )
    val easedScale by animateFloatAsState(
        targetValue = if (isPlaying) scale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "artScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center,
    ) {
        // هاله رنگی پشت آرت
        Box(
            modifier = Modifier
                .size(250.dp)
                .scale(if (isPlaying) 1.12f else 1f)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.32f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f)
                .scale(easedScale)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White.copy(alpha = 0.07f)),
        ) {
            if (hasCurrent) {
                SongArtworkFill(
                    albumId = albumId,
                    songId = songId,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(27.dp)),
                )
            } else {
                GlassSurface(modifier = Modifier.fillMaxSize(), cornerRadius = 27.dp) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center),
                    )
                }
            }
        }
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
    val fraction = if (durationMs > 0) (displayPosition.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column {
            androidx.compose.material3.Slider(
                value = fraction,
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    dragFraction?.let { f -> onSeek((f * durationMs).toLong()) }
                    dragFraction = null
                },
                enabled = durationMs > 0,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    activeTrackColor = SpColors.Amber,
                    inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                    thumbColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
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
        // کنترل‌های اصلی — طبق استاندارد جهانی LTR: قبلی چپ، بعدی راست
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlIcon(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = if (shuffleEnabled) "خروج از پخش تصادفی" else "پخش تصادفی",
                    active = shuffleEnabled,
                    onClick = onToggleShuffle,
                )
                ControlIcon(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = "آهنگ قبلی",
                    iconSize = 44.dp,
                    onClick = onPrevious,
                )
                GlowPlayButton(
                    isPlaying = isPlaying,
                    onClick = onTogglePlayPause,
                )
                ControlIcon(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "آهنگ بعدی",
                    iconSize = 44.dp,
                    onClick = onNext,
                )
                ControlIcon(
                    icon = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = repeatLabel(repeatMode),
                    active = repeatActive,
                    onClick = onCycleRepeat,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassPillButton(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavorite) "حذف از علاقه‌مندی‌ها" else "افزودن به علاقه‌مندی‌ها",
                tint = if (isFavorite) SpColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onToggleFavorite,
            )
            GlassPillButton(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "نمایش صف پخش",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onOpenQueue,
            )
        }
    }
}

/** دکمه پخش با گرادیان و هاله نور — قلب بصری صفحه */
@Composable
private fun GlowPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val glowPulse by animateFloatAsState(
        targetValue = if (isPlaying) 1.18f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "glow",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.86f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "iconScale",
    )

    Box(contentAlignment = Alignment.Center) {
        // هاله
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(glowPulse)
                .background(
                    Brush.radialGradient(
                        listOf(SpColors.Glow, Color.Transparent),
                    ),
                ),
        )
        // دکمه گرادیانی
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(SpColors.AmberGradient),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "توقف" else "پخش",
                    tint = Color(0xFF221600),
                    modifier = Modifier
                        .size(48.dp)
                        .scale(iconScale),
                )
            }
        }
    }
}

@Composable
private fun ControlIcon(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean = false,
    iconSize: androidx.compose.ui.unit.Dp = 30.dp,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (active) SpColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "controlTint",
    )
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun GlassPillButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.06f),
        modifier = Modifier.size(46.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(24.dp))
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
    GlassSurface(cornerRadius = 32.dp, fillAlpha = 0.10f) {
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
                    tint = SpColors.Amber,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "حالت خودرو فعال است",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SpColors.Amber,
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
            // کنترل‌های خودرو هم استاندارد جهانی LTR: قبلی چپ، بعدی راست
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CarButton(Icons.Rounded.SkipPrevious, "آهنگ قبلی", 92.dp, 44.dp, onClick = onPrevious)
                    Box(
                        modifier = Modifier
                            .size(122.dp)
                            .clip(CircleShape)
                            .background(SpColors.AmberGradient),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(onClick = onTogglePlayPause, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "توقف" else "پخش",
                                tint = Color(0xFF221600),
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                    CarButton(Icons.Rounded.SkipNext, "آهنگ بعدی", 92.dp, 44.dp, onClick = onNext)
                }
            }
            Spacer(Modifier.height(20.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = {
                    if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                },
                modifier = Modifier.fillMaxWidth(),
                color = SpColors.Amber,
                trackColor = Color.White.copy(alpha = 0.14f),
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

@Composable
private fun CarButton(
    icon: ImageVector,
    contentDescription: String,
    buttonSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    androidx.compose.material3.FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(buttonSize),
        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize))
    }
}

private fun repeatLabel(mode: Int): String = when (mode) {
    androidx.media3.common.Player.REPEAT_MODE_ALL -> "تکرار همه"
    androidx.media3.common.Player.REPEAT_MODE_ONE -> "تکرار همین آهنگ"
    else -> "بدون تکرار"
}

/** حداقل جابه‌جایی افقی (پیکسل) برای شمردن یک سوایپ تغییر آهنگ */
private const val SWIPE_THRESHOLD_PX = 120f
