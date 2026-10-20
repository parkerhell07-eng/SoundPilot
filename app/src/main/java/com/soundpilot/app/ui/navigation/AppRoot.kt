package com.soundpilot.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.soundpilot.app.ui.components.MiniPlayerBar
import com.soundpilot.app.ui.equalizer.EqualizerScreen
import com.soundpilot.app.ui.library.CollectionDetailScreen
import com.soundpilot.app.ui.library.LibraryScreen
import com.soundpilot.app.ui.player.PlaybackViewModel
import com.soundpilot.app.ui.player.PlayerScreen
import com.soundpilot.app.ui.settings.SettingsScreen

object Routes {
    const val LIBRARY = "library"
    const val EQUALIZER = "equalizer"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val ALBUM = "album"
    const val ARTIST = "artist"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.LIBRARY, "کتابخانه", Icons.Rounded.LibraryMusic),
    BottomDestination(Routes.EQUALIZER, "اکولایزر", Icons.Rounded.GraphicEq),
    BottomDestination(Routes.SETTINGS, "تنظیمات", Icons.Rounded.Settings),
)

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun SoundPilotApp(playbackVm: PlaybackViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isPlaying by playbackVm.isPlaying.collectAsStateWithLifecycle()
    val currentMetadata by playbackVm.currentMetadata.collectAsStateWithLifecycle()
    val currentSong by playbackVm.currentSong.collectAsStateWithLifecycle()
    val connectionState by playbackVm.connectionState.collectAsStateWithLifecycle()
    val hasCurrent = currentMetadata != null

    val snackbarHostState = remember { SnackbarHostState() }

    // نمایش سراسری همه خطاهای پخش — هیچ خطایی دیگر بی‌صدا نمی‌ماند
    LaunchedEffect(Unit) {
        playbackVm.errors.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    val showChrome = currentRoute in setOf(Routes.LIBRARY, Routes.EQUALIZER, Routes.SETTINGS)
    val showMiniPlayer = showChrome && hasCurrent

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // بنر وضعیت اتصال به موتور پخش — کاربر هرگز «بدون واکنش» نمی‌ماند
            AnimatedVisibility(visible = showChrome && connectionState != PlaybackViewModel.ConnectionState.READY) {
                ConnectionBanner(
                    failed = connectionState == PlaybackViewModel.ConnectionState.FAILED,
                    onRetry = playbackVm::retryConnection,
                )
            }
        },
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    MiniPlayerBar(
                        title = currentMetadata?.title?.toString() ?: "",
                        artist = currentMetadata?.artist?.toString() ?: "",
                        albumId = currentSong?.albumId ?: 0L,
                        songId = currentSong?.id ?: 0L,
                        isPlaying = isPlaying,
                        onTogglePlay = playbackVm::togglePlayPause,
                        onNext = playbackVm::next,
                        onOpen = { navController.navigate(Routes.PLAYER) },
                    )
                }
                AnimatedVisibility(visible = showChrome) {
                    NavigationBar {
                        bottomDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = { navController.navigateToTab(destination.route) },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                    )
                                },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LIBRARY,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(androidx.compose.animation.core.tween(180)) },
            exitTransition = { fadeOut(androidx.compose.animation.core.tween(180)) },
        ) {
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    playbackVm = playbackVm,
                    onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                    onOpenAlbum = { navController.navigate("${Routes.ALBUM}/$it") },
                    onOpenArtist = {
                        navController.navigate("${Routes.ARTIST}/${Uri.encode(it)}")
                    },
                )
            }
            composable(Routes.EQUALIZER) {
                EqualizerScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.PLAYER,
                enterTransition = {
                    slideInVertically(androidx.compose.animation.core.tween(280)) { it } +
                        fadeIn(androidx.compose.animation.core.tween(280))
                },
                exitTransition = {
                    slideOutVertically(androidx.compose.animation.core.tween(280)) { it } +
                        fadeOut(androidx.compose.animation.core.tween(280))
                },
            ) {
                PlayerScreen(playbackVm = playbackVm, onBack = { navController.popBackStack() })
            }
            composable(
                route = "${Routes.ALBUM}/{albumId}",
                arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
            ) { entry ->
                CollectionDetailScreen(
                    albumId = entry.arguments?.getLong("albumId") ?: 0L,
                    playbackVm = playbackVm,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(route = "${Routes.ARTIST}/{name}") { entry ->
                CollectionDetailScreen(
                    artistName = entry.arguments?.getString("name") ?: "",
                    playbackVm = playbackVm,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** بنر اتصال: در حال اتصال (اسپینر) یا اتصال ناموفق (دکمه تلاش مجدد) */
@Composable
private fun ConnectionBanner(
    failed: Boolean,
    onRetry: () -> Unit,
) {
    Surface(
        color = if (failed) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (failed) {
                Icon(
                    Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            Text(
                text = if (failed) {
                    "اتصال به سرویس پخش برقرار نشد"
                } else {
                    "در حال آماده‌سازی موتور پخش…"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (failed) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            )
            if (failed) {
                TextButton(onClick = onRetry) {
                    Text("تلاش مجدد")
                }
            }
        }
    }
}
