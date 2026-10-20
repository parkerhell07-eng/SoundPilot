package com.soundpilot.app.ui.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
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
import androidx.palette.graphics.Palette
import com.google.common.util.concurrent.ListenableFuture
import com.soundpilot.app.data.AlbumArtRepository
import com.soundpilot.app.data.LastPlayed
import com.soundpilot.app.data.LibraryState
import com.soundpilot.app.data.LibraryStore
import com.soundpilot.app.data.SettingsRepository
import com.soundpilot.app.di.ServiceLocator
import com.soundpilot.app.domain.SleepTimer
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * مدیریت وضعیت پخش در لایه UI با MediaController و StateFlow.
 * یک نمونه در سطح Activity ساخته می‌شود و بین همه صفحه‌ها به اشتراک گذاشته می‌شود.
 *
 * سخت‌سازی‌های ضد «بدون واکنش»:
 * - اتصال با timeout و تلاش مجدد خودکار (تا ۴ بار) انجام می‌شود.
 * - اگر کاربر هنگام اتصال روی آهنگی بزند، درخواست در صف نگه داشته می‌شود و
 *   بلافاصله پس از برقراری اتصال اجرا می‌شود — هیچ لمسی گم نمی‌شود.
 * - همه خطاها به لایه UI (Snackbar سراسری) منتشر می‌شوند؛ هیچ شکست بی‌صدایی وجود ندارد.
 */
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val settings: SettingsRepository = ServiceLocator.settingsRepository

    // اسکوپ مستقل برای ذخیره‌سازی نهایی پس از نابودی ViewModel
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var connectJob: Job? = null
    private var connectAttempts = 0

    private val sleepTimer = SleepTimer(viewModelScope)

    private val _controller = MutableStateFlow<MediaController?>(null)

    enum class ConnectionState { CONNECTING, READY, FAILED }

    data class QueueEntry(
        val index: Int,
        val mediaId: Long?,
        val albumId: Long,
        val title: String,
        val artist: String,
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

    /** آهنگ جاری از روی کتابخانه — برای آرت‌ورک و جزئیات UI */
    val currentSong: StateFlow<Song?> = combine(
        _currentSongId,
        LibraryStore.state,
    ) { id, store ->
        val songs = (store as? LibraryState.Ready)?.songs ?: emptyList()
        if (id == null) null else songs.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    val favorites: StateFlow<Set<Long>> = settings.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** پرش خودکار از سکوت‌های طولانی — برای شنیدن پیوسته در خودرو */
    val skipSilence: StateFlow<Boolean> = settings.skipSilence
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** سرعت پخش با حفظ گام صدا */
    val playbackSpeed: StateFlow<Float> = settings.playbackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1f)

    /** وضعیت تایمر خواب */
    val sleepTimerState: StateFlow<SleepTimer.State> = sleepTimer.state

    /** رنگ استخراج‌شده از کاور آهنگ جاری — برای پس‌زمینه دینامیک صفحه پخش */
    val playerPalette: StateFlow<Int?> = currentSong
        .map { song ->
            if (song == null) {
                null
            } else {
                extractPaletteColor(AlbumArtRepository.bitmapFor(song.albumId, song.id))
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** درخواست پخش که هنگام اتصال دریافت شده و پس از اتصال اجرا می‌شود */
    private var pendingPlay: Pair<List<Song>, Int>? = null

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
            // تور ایمنی: در حالت «پایان همین آهنگ» اگر گذار به آهنگ بعدی انجام شد، فوراً پاز کن
            if (sleepTimer.state.value.mode == SleepTimer.Mode.END_OF_TRACK) {
                _controller.value?.pause()
                sleepTimer.finishEndOfTrack {}
            }
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
                    // حالت «خواب در پایان همین آهنگ»: پیش‌بینی نزدیک‌شدن به پایان و پاز دقیق
                    if (sleepTimer.state.value.mode == SleepTimer.Mode.END_OF_TRACK) {
                        maybePauseAtEndOfTrack(c)
                    }
                }
            }
        }
        observeOutputDevices()
    }

    // ---------------- اتصال به سرویس پخش ----------------

    private fun connect() {
        if (_connectionState.value == ConnectionState.READY) return
        if (connectJob?.isActive == true) return
        connectJob = viewModelScope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            try {
                val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
                val controller = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                    MediaController.Builder(appContext, token)
                        .buildAsync()
                        .await()
                }
                if (controller == null) {
                    throw IllegalStateException("اتصال به سرویس پخش بیشتر از ${CONNECT_TIMEOUT_MS / 1000} ثانیه طول کشید")
                }
                controller.addListener(playerListener)
                _controller.value = controller
                connectAttempts = 0
                _connectionState.value = ConnectionState.READY
                snapshot(controller)
                restoreIfNeeded(controller)
                applyTuning(controller)
                runPendingPlayIfReady()
            } catch (e: Exception) {
                Log.e(TAG, "اتصال به سرویس پخش ناموفق بود (تلاش ${connectAttempts + 1})", e)
                connectAttempts++
                if (connectAttempts < MAX_CONNECT_ATTEMPTS) {
                    delay(CONNECT_RETRY_DELAY_MS * connectAttempts)
                    connectJob = null
                    connect()
                } else {
                    _connectionState.value = ConnectionState.FAILED
                    _errors.tryEmit("اتصال به سرویس پخش برقرار نشد. از دکمه «تلاش مجدد» استفاده کنید.")
                }
            }
        }
    }

    /** تلاش مجدد کاربر یا خودکار برای اتصال به سرویس پخش */
    fun retryConnection() {
        if (_connectionState.value == ConnectionState.READY) return
        connectAttempts = 0
        connectJob = null
        connect()
    }

    private fun runPendingPlayIfReady() {
        val (songs, index) = pendingPlay ?: return
        pendingPlay = null
        playInternal(songs, index)
    }

    // ---------------- اسنپ‌شات وضعیت ----------------

    private var lastQueueSignature: String = ""

    private fun snapshot(player: Player) {
        _isPlaying.value = player.isPlaying
        val item = player.currentMediaItem
        _currentMetadata.value = item?.mediaMetadata
        _currentSongId.value = item?.mediaId?.toLongOrNull()
        _durationMs.value = if (player.duration == C.TIME_UNSET) 0L else player.duration.coerceAtLeast(0)
        _shuffleEnabled.value = player.shuffleModeEnabled
        _repeatMode.value = player.repeatMode
        if (player.playbackState == Player.STATE_IDLE) _positionMs.value = 0L
        rebuildQueueIfChanged(player)
    }

    /** صف فقط وقتی بازسازی می‌شود که واقعاً تغییر کرده باشد (بهبود کارایی) */
    private fun rebuildQueueIfChanged(player: Player) {
        val count = player.mediaItemCount
        val signature = "$count:${player.currentMediaItemIndex}:${
            buildString {
                for (i in 0 until count) append(player.getMediaItemAt(i).mediaId).append(',')
            }
        }"
        if (signature == lastQueueSignature) return
        lastQueueSignature = signature
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
                albumId = item.mediaMetadata.extras?.getLong(EXTRA_ALBUM_ID_KEY) ?: 0L,
                title = item.mediaMetadata.title?.toString() ?: "بدون عنوان",
                artist = item.mediaMetadata.artist?.toString() ?: "",
                isCurrent = i == currentIndex,
            )
        }
    }

    // ---------------- فرمان‌های پخش ----------------

    /**
     * شروع پخش یک فهرست از نقطه مشخص — کل صف جایگزین می‌شود.
     * اگر سرویس هنوز متصل نیست، درخواست ذخیره و پس از اتصال اجرا می‌شود.
     */
    fun play(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        when (_connectionState.value) {
            ConnectionState.READY -> playInternal(songs, startIndex)
            else -> {
                pendingPlay = songs to startIndex
                _errors.tryEmit("در حال آماده‌سازی موتور پخش…")
                if (_connectionState.value == ConnectionState.FAILED) retryConnection()
            }
        }
    }

    private fun playInternal(songs: List<Song>, startIndex: Int) {
        val c = _controller.value ?: run {
            pendingPlay = songs to startIndex
            retryConnection()
            return
        }
        val start = startIndex.coerceIn(0, songs.lastIndex)
        // گرم‌کردن کاور آهنگ شروع (غیرمسدودکننده) تا اعلان/صف سریع‌تر تصویر داشته باشند
        songs.getOrNull(start)?.let { AlbumArtRepository.warmUp(it.albumId, it.id) }
        lastQueueSignature = "" // اجبار به بازسازی صف
        c.setMediaItems(songs.map { it.toMediaItem() }, start, 0L)
        c.prepare()
        c.play()
    }

    fun playNext(song: Song) {
        val c = _controller.value ?: run {
            _errors.tryEmit("برای «پخش بعدی» ابتدا یک آهنگ پخش کنید.")
            return
        }
        if (c.mediaItemCount == 0) {
            play(listOf(song), 0)
            return
        }
        AlbumArtRepository.warmUp(song.albumId, song.id)
        c.addMediaItems(c.currentMediaItemIndex + 1, listOf(song.toMediaItem()))
    }

    fun addToQueue(song: Song) {
        val c = _controller.value ?: run {
            _errors.tryEmit("برای افزودن به صف ابتدا یک آهنگ پخش کنید.")
            return
        }
        if (c.mediaItemCount == 0) {
            play(listOf(song), 0)
            return
        }
        AlbumArtRepository.warmUp(song.albumId, song.id)
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
        when {
            c.hasNextMediaItem() -> c.seekToNextMediaItem()
            c.repeatMode == Player.REPEAT_MODE_ALL && c.mediaItemCount > 0 -> {
                c.seekTo(0, 0L)
                c.prepare()
                c.play()
            }
        }
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
            c.prepare()
            c.play()
        }
    }

    fun removeFromQueue(index: Int) {
        val c = _controller.value ?: return
        if (index in 0 until c.mediaItemCount) {
            lastQueueSignature = ""
            c.removeMediaItem(index)
        }
    }

    fun toggleFavorite(songId: Long?) {
        if (songId == null || songId <= 0) return
        viewModelScope.launch { settings.toggleFavorite(songId) }
    }

    // ---------------- تنظیمات پخش (سرعت/سکوت) ----------------

    /** بازیابی تنظیمات پخش ذخیره‌شده و اعمال روی پلیر پس از اتصال */
    private suspend fun applyTuning(c: MediaController) {
        val speed = settings.playbackSpeed.first()
        runCatching {
            if (abs(speed - 1f) > 0.01f) {
                c.playbackParameters = c.playbackParameters.withSpeed(speed)
            }
        }.onFailure { Log.w(TAG, "اعمال تنظیمات پخش ناموفق بود", it) }
    }

    fun setSkipSilence(value: Boolean) {
        // پرش از سکوت فقط از طریق ExoPlayer سرویس اعمال می‌شود؛
        // سرویس این تنظیم را به‌صورت زنده collect می‌کند.
        viewModelScope.launch { settings.setSkipSilence(value) }
    }

    fun setPlaybackSpeed(value: Float) {
        val v = value.coerceIn(
            SettingsRepository.MIN_PLAYBACK_SPEED,
            SettingsRepository.MAX_PLAYBACK_SPEED,
        )
        viewModelScope.launch {
            settings.setPlaybackSpeed(v)
            _controller.value?.let { it.playbackParameters = it.playbackParameters.withSpeed(v) }
        }
    }

    // ---------------- تایمر خواب ----------------

    fun startSleepTimer(durationMs: Long) {
        sleepTimer.start(durationMs) {
            _controller.value?.let { c -> if (c.isPlaying) c.pause() }
        }
    }

    fun setSleepAtEndOfTrack() {
        sleepTimer.markEndOfTrack()
    }

    fun cancelSleepTimer() {
        sleepTimer.cancel()
    }

    /**
     * حالت «پایان همین آهنگ» — MediaController به setPauseAtEndOfMediaItems دسترسی ندارد،
     * پس پایان آهنگ را پیش‌بینی می‌کنیم: با نزدیک‌شدن به ۴۰۰ms آخر، پاز می‌کنیم؛
     * تور ایمنی دوم روی onMediaItemTransition است.
     */
    private fun maybePauseAtEndOfTrack(c: MediaController) {
        if (!c.isPlaying) return
        val duration = c.duration
        if (duration <= 0) return
        val remaining = duration - c.currentPosition
        if (remaining in 0..400) {
            c.pause()
            sleepTimer.finishEndOfTrack {}
        }
    }

    private suspend fun extractPaletteColor(bitmap: Bitmap?): Int? {
        if (bitmap == null) return null
        return try {
            val palette = Palette.from(bitmap).maximumColorCount(24).generate()
            listOfNotNull(
                palette.darkVibrantSwatch,
                palette.vibrantSwatch,
                palette.darkMutedSwatch,
                palette.mutedSwatch,
            ).maxByOrNull { it.population }?.rgb
        } catch (e: Exception) {
            Log.d(TAG, "استخراج رنگ کاور ناموفق بود", e)
            null
        } finally {
            runCatching { bitmap.recycle() }
        }
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
        lastQueueSignature = ""
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
        connectJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onCleared()
    }

    companion object {
        private const val TAG = "PlaybackViewModel"
        private const val CONNECT_TIMEOUT_MS = 6_000L
        private const val MAX_CONNECT_ATTEMPTS = 4
        private const val CONNECT_RETRY_DELAY_MS = 800L
        private const val EXTRA_ALBUM_ID_KEY = "com.soundpilot.app.ALBUM_ID"
    }
}
