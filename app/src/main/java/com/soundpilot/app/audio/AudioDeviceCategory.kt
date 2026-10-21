package com.soundpilot.app.audio

/**
 * رده‌بندی خروجی صوتی — منطق خالص و قابل تست واحد.
 * هر رده می‌تواند پروفایل صوتی مستقل خودش را در DataStore داشته باشد؛
 * مثلاً «خودروی استاندارد» روی بلوتوث ماشین و «متعادل» روی بلندگوی گوشی.
 */
enum class AudioDeviceCategory(val label: String) {
    SPEAKER("بلندگوی گوشی"),
    WIRED("هدفون سیمی"),
    BLUETOOTH("بلوتوث"),
    USB("USB صوتی"),
    HDMI("HDMI"),
    OTHER("خروجی دیگر"),
    ;

    companion object {
        /**
         * نگاشت نوع دستگاه اندروید (AudioDeviceInfo.type) به رده.
         * ثابت‌ها همان AudioDeviceInfo.TYPE_* هستند تا منطق بدون اندروید تست شود.
         */
        fun fromDeviceType(type: Int): AudioDeviceCategory = when (type) {
            2 /* TYPE_BUILTIN_SPEAKER */,
            1 /* TYPE_BUILTIN_EARPIECE */ -> SPEAKER

            3 /* TYPE_WIRED_HEADSET */,
            4 /* TYPE_WIRED_HEADPHONES */ -> WIRED

            8 /* TYPE_BLUETOOTH_A2DP */,
            7 /* TYPE_BLUETOOTH_SCO */,
            26 /* TYPE_BLE_HEADSET */,
            27 /* TYPE_BLE_SPEAKER */ -> BLUETOOTH

            11 /* TYPE_USB_DEVICE */,
            12 /* TYPE_USB_ACCESSORY */,
            13 /* TYPE_USB_HEADSET */ -> USB

            9 /* TYPE_HDMI */,
            10 /* TYPE_HDMI_ARC */ -> HDMI

            else -> OTHER
        }
    }
}
