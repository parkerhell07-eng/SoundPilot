package com.soundpilot.app.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorMapperTest {

    @Test
    fun `missing file maps to a clear message`() {
        val message = PlaybackErrorMapper.message(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
        assertTrue(message.contains("پیدا نشد"))
    }

    @Test
    fun `corrupt or unsupported file maps to decode message`() {
        val message = PlaybackErrorMapper.message(PlaybackException.ERROR_CODE_DECODING_FAILED)
        assertTrue(message.contains("قابل پخش نیست"))
        val message2 = PlaybackErrorMapper.message(
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        )
        assertEquals(
            PlaybackErrorMapper.message(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED),
            message2,
        )
    }

    @Test
    fun `audio output failure mentions checking the sound device`() {
        val message = PlaybackErrorMapper.message(PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED)
        assertTrue(message.contains("دستگاه صوتی"))
    }

    @Test
    fun `unknown error codes still produce a non blank message`() {
        val message = PlaybackErrorMapper.message(-12345)
        assertTrue(message.isNotBlank())
        assertTrue(message.contains("-12345"))
    }

    @Test
    fun `timeout has its own message`() {
        val message = PlaybackErrorMapper.message(PlaybackException.ERROR_CODE_TIMEOUT)
        assertTrue(message.contains("زمان"))
    }
}
