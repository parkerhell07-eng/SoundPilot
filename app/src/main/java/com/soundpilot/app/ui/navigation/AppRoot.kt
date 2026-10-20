package com.soundpilot.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    val hasCurrent = currentMetadata != null

    val showChrome = currentRoute in setOf(Routes.LIBRARY, Routes.EQUALIZER, Routes.SETTINGS)
    val showMiniPlayer = showChrome && hasCurrent

    Scaffold(
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
                        artworkUri = currentMetadata?.artworkUri?.toString(),
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
