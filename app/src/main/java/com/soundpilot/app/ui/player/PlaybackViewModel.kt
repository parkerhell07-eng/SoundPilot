package com.soundpilot.app.ui.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.soundpilot.app.data.LastPlayed
import com.soundpilot.app.data.LibraryStore
import com.soundpilot.app.data.SettingsRepository
import com.soundpilot.app.di.ServiceLocator
import com.soundpilot.app.domain.model.Song
import com.soundpilot.app.playback.PlaybackErrorMapper
import com.soundpilot.app.playback.PlaybackService
import com.soundpilot.app.playback.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * مدیریت وضعیت پخش در لایه UI با MediaController و StateFlow.
 * یک نمونه در سطح Activity ساخته می‌شود و بین همه صفحه‌ها به اشتراک گذاشته می‌شود.
 */
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settings: SettingsRepository = ServiceLocator.settingsRepository

    // اسکوپ مستقل برای ذخیره‌سازی نهایی پس از نابودی ViewModel
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _controller = MutableStateFlow<MediaController?>(null)

    enum class ConnectionState { CONNECTING, READY, FAILED }

    data class QueueEntry(
        val index: Int,
        val mediaId: Long?,
        val title: String,
        val artist: String,
        val artworkUri: String?,
        val isCurrent: Boolean,
    )

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTING)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMetadata = MutableStateFlow<MediaMetadata?>(null)
    val currentMetadata: StateFlow<MediaMetadata?> = _currentMetadata.asStateFlow()

    private val _currentSongId = MutableStateFlow<Long?>(null)
    val currentSongId: StateFlow<Long?> = _currentSongId.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _outputDeviceName = MutableStateFlow("")
    val outputDeviceName: StateFlow<String> = _outputDeviceName.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    val favorites: StateFlow<Set<Long>> = settings.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            snapshot(player)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            _positionMs.value = newPosition.positionMs.coerceAtLeast(0)
            if (reason == Player.DISCONTINUITY_REASON_SEEK) scheduleSaveLastPlayed()
        }

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            scheduleSaveLastPlayed()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) scheduleSaveLastPlayed()
        }

        override fun onPlayerError(error: PlaybackException) {
            _errors.tryEmit(PlaybackErrorMapper.message(error.errorCode))
        }
    }

    init {
        connect()
        viewModelScope.launch {
            while (true) {
                delay(500)
                _controller.value?.let { c ->
                    if (c.isPlaying) _positionMs.value = c.currentPosition.coerceAtLeast(0)
                }
            }
        }
        observeOutputDevices()
    }

    private fun connect() {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    controller.addListener(playerListener)
                    _connectionState.value = ConnectionState.READY
                    snapshot(controller)
                    viewModelScope.launch { restoreIfNeeded(controller) }
                } catch (e: Exception) {
                    Log.e(TAG, "اتصال به سرویس پخش ناموفق بود", e)
                    _connectionState.value = ConnectionState.FAILED
                    _errors.tryEmit("اتصال به سرویس پخش برقرار نشد. دوباره تلاش کنید.")
                }
            },
            ContextCompat.getMainExecutor(appContext),
        )
    }

    /** تلاش مجدد برای اتصال به سرویس پخش */
    fun retryConnection() {
        if (_connectionState.value == ConnectionState.READY) return
        connect()
    }

    private fun snapshot(player: Player) {
        _isPlaying.value = player.isPlaying
        val item = player.currentMediaItem
        _currentMetadata.value = item?.mediaMetadata
        _currentSongId.value = item?.mediaId?.toLongOrNull()
        _durationMs.value = if (player.duration == C.TIME_UNSET) 0L else player.duration.coerceAtLeast(0)
        _shuffleEnabled.value = player.shuffleModeEnabled
        _repeatMode.value = player.repeatMode
        if (player.playbackState == Player.STATE_IDLE) _positionMs.value = 0L
        _queue.value = buildQueue(player)
    }

    private fun buildQueue(player: Player): List<QueueEntry> {
        val count = player.mediaItemCount
        if (count == 0) return emptyList()
        val currentIndex = player.currentMediaItemIndex
        return (0 until count).map { i ->
            val item = player.getMediaItemAt(i)
            QueueEntry(
                index = i,
                mediaId = item.mediaId.toLongOrNull(),
                title = item.mediaMetadata.title?.toString() ?: "بدون عنوان",
                artist = item.mediaMetadata.artist?.toString() ?: "",
                artworkUri = item.mediaMetadata.artworkUri?.toString(),
                isCurrent = i == currentIndex,
            )
        }
    }

    // ---------------- فرمان‌های پخش ----------------

    /** شروع پخش یک فهرست از نقطه مشخص — کل صف جایگزین می‌شود */
    fun play(songs: List<Song>, startIndex: Int) {
        val c = _controller.value ?: return
        if (songs.isEmpty()) return
        val start = startIndex.coerceIn(0, songs.lastIndex)
        c.setMediaItems(songs.map { it.toMediaItem() }, start, 0L)
        c.prepare()
        c.play()
    }

    fun playNext(song: Song) {
        val c = _controller.value ?: return
        c.addMediaItems(c.currentMediaItemIndex + 1, listOf(song.toMediaItem()))
    }

    fun addToQueue(song: Song) {
        val c = _controller.value ?: return
        c.addMediaItems(listOf(song.toMediaItem()))
    }

    fun togglePlayPause() {
        val c = _controller.value ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() {
        val c = _controller.value ?: return
        if (c.hasNextMediaItem() || c.repeatMode == Player.REPEAT_MODE_ALL) c.seekToNextMediaItem()
    }

    fun previous() {
        val c = _controller.value ?: return
        if (c.currentPosition > 3_000) {
            c.seekTo(0)
        } else {
            c.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        val c = _controller.value ?: return
        _positionMs.value = positionMs.coerceAtLeast(0)
        c.seekTo(positionMs.coerceAtLeast(0))
    }

    fun toggleShuffle() {
        val c = _controller.value ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val c = _controller.value ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun jumpTo(index: Int) {
        val c = _controller.value ?: return
        if (index in 0 until c.mediaItemCount) {
            c.seekToDefaultPosition(index)
            c.play()
        }
    }

    fun removeFromQueue(index: Int) {
        val c = _controller.value ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun toggleFavorite(songId: Long?) {
        if (songId == null || songId <= 0) return
        viewModelScope.launch { settings.toggleFavorite(songId) }
    }

    // ---------------- بازیابی و ذخیره وضعیت ----------------

    private suspend fun restoreIfNeeded(c: MediaController) {
        if (c.mediaItemCount > 0) return
        if (!settings.restoreLastPlayed.first()) return
        val last = settings.lastPlayed.first() ?: return
        LibraryStore.ensureLoaded()
        val songs = last.songIds.mapNotNull { LibraryStore.songById(it) }
        if (songs.isEmpty()) {
            settings.clearLastPlayed()
            return
        }
        val items = songs.map { it.toMediaItem() }
        val index = last.index.coerceIn(0, items.lastIndex)
        c.setMediaItems(items, index, last.positionMs.coerceAtLeast(0L))
        c.repeatMode = last.repeatMode
        c.shuffleModeEnabled = last.shuffle
        c.prepare()
        // عمداً play() نمی‌زنیم؛ کاربر خودش ادامه می‌دهد
    }

    private var saveJob: Job? = null

    private fun scheduleSaveLastPlayed() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            saveLastPlayedNow()
        }
    }

    private suspend fun saveLastPlayedNow() {
        val c = _controller.value ?: return
        if (c.mediaItemCount == 0) return
        settings.saveLastPlayed(
            LastPlayed(
                songIds = (0 until c.mediaItemCount)
                    .mapNotNull { c.getMediaItemAt(it).mediaId.toLongOrNull() },
                index = c.currentMediaItemIndex,
                positionMs = c.currentPosition.coerceAtLeast(0),
                shuffle = c.shuffleModeEnabled,
                repeatMode = c.repeatMode,
                savedAt = System.currentTimeMillis(),
            ),
        )
    }

    // ---------------- دستگاه خروجی ----------------

    private fun observeOutputDevices() {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        fun describe(): String {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val bluetooth = outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }
            if (bluetooth != null) {
                val name = bluetooth.productName?.toString()?.takeIf { it.isNotBlank() }
                return if (name != null) "بلوتوث — $name" else "بلوتوث"
            }
            val wired = outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
            if (wired != null) return "هدفون سیمی"
            return "بلندگوی گوشی"
        }

        _outputDeviceName.value = describe()
        audioManager.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                    _outputDeviceName.value = describe()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                    _outputDeviceName.value = describe()
                }
            },
            Handler(Looper.getMainLooper()),
        )
    }

    override fun onCleared() {
        val c = _controller.value
        if (c != null && c.mediaItemCount > 0) {
            appScope.launch { saveLastPlayedNow() }
        }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onCleared()
    }

    companion object {
        private const val TAG = "PlaybackViewModel"
    }
}
