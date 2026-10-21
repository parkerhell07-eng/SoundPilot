package com.soundpilot.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.soundpilot.app.MainActivity
import com.soundpilot.app.audio.AudioEffectsController
import com.soundpilot.app.audio.AudioEffectsHub
import com.soundpilot.app.audio.EqState
import com.soundpilot.app.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * سرویس پخش پس‌زمینه بر پایه MediaSessionService.
 *
 * - پخش پایدار در پس‌زمینه با اعلان رسانه و کنترل از صفحه قفل
 * - مدیریت خودکار فوکوس صوتی (پاز هنگام تماس/راهنمای ناوبری) و قطع هدفون (becoming noisy)
 * - اتصال افکت‌های صوتی به نشست صوتی واقعی پلیر
 * - ریکاوری خودکار از خطای پخش (پرش به آهنگ بعدی؛ توقف در صورت خرابی همه)
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var serviceScope: CoroutineScope? = null
    private var effects: AudioEffectsController? = null
    private var consecutiveErrors = 0

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        effects = AudioEffectsController(this, serviceScope!!, ServiceLocator.settingsRepository).also {
            AudioEffectsHub.attachController(it)
        }

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                effects?.attach(audioSessionId)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                consecutiveErrors = 0
            }

            override fun onPlayerError(error: PlaybackException) {
                handlePlaybackError(player, error)
            }
        })

        // مسیر اتصال افکت کمکی (طنین) به AudioTrack پلیر
        effects?.auxEffectSink = { info ->
            runCatching {
                if (info == null) player.clearAuxEffectInfo() else player.setAuxEffectInfo(info)
            }
        }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        // بازیابی وضعیت ذخیره‌شده افکت‌ها و اتصال به نشست فعلی (در صورت وجود)
        serviceScope!!.launch {
            val saved = ServiceLocator.settingsRepository.equalizerSettings.first()
            AudioEffectsHub.setDesiredState(
                EqState(
                    enabled = saved.enabled,
                    profileId = saved.profileId,
                    gainsDb = saved.gainsDb,
                    bassStrength = saved.bassStrength,
                    virtualizerStrength = saved.virtualizerStrength,
                    loudnessGainMb = saved.loudnessGainMb,
                    reverbId = saved.reverbId,
                    reverbAmount = saved.reverbAmount,
                ),
            )
            if (player.audioSessionId != 0) {
                effects?.attach(player.audioSessionId)
            }
            // تنظیمات پخش به‌صورت زنده از سمت سرویس اعمال می‌شود (skipSilence فقط
            // روی ExoPlayer قابل تنظیم است، نه MediaController)
            val settings = ServiceLocator.settingsRepository
            serviceScope!!.launch {
                settings.skipSilence.collect { value ->
                    runCatching { player.skipSilenceEnabled = value }
                }
            }
            serviceScope!!.launch {
                settings.playbackSpeed.collect { value ->
                    runCatching {
                        player.playbackParameters = player.playbackParameters.withSpeed(value)
                    }
                }
            }
        }
    }

    private fun handlePlaybackError(player: Player, error: PlaybackException) {
        consecutiveErrors++
        Log.w(TAG, "خطای پخش: ${error.errorCodeName}", error)
        val total = player.mediaItemCount
        if (total > 1 && consecutiveErrors < total) {
            // فایل خراب را رد کن و ادامه بده
            player.seekToNextMediaItem()
            player.prepare()
            player.play()
        } else {
            // همه آیتم‌ها مشکل دارند یا فقط یکی در صف است
            player.pause()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope?.cancel()
        serviceScope = null
        effects?.release()
        effects = null
        AudioEffectsHub.attachController(null)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PlaybackService"
    }
}
