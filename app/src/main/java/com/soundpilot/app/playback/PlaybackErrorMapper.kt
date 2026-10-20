package com.soundpilot.app.playback

import androidx.media3.common.PlaybackException

/**
 * ترجمه کدهای خطای PlaybackException به پیام فارسی قابل فهم برای کاربر.
 * (منطق خالص — پوشش داده‌شده در تست‌های واحد)
 */
object PlaybackErrorMapper {

    fun message(errorCode: Int): String = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
            "فایل موسیقی پیدا نشد؛ ممکن است حذف یا جابه‌جا شده باشد."

        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
        -> "خواندن فایل موسیقی ممکن نشد."

        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> "این فایل قابل پخش نیست؛ فرمت پشتیبانی نمی‌شود یا فایل خراب است."

        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
        -> "خطای خروجی صدا؛ اتصال دستگاه صوتی (بلوتوث یا هدفون) را بررسی کنید."

        PlaybackException.ERROR_CODE_TIMEOUT ->
            "زمان خواندن فایل به پایان رسید."

        else -> "خطای ناشناخته در پخش (کد $errorCode)"
    }
}
