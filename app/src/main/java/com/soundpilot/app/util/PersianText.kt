package com.soundpilot.app.util

import java.util.Locale

/**
 * ابزارهای متن فارسی: نرمال‌سازی برای جست‌وجو، اعداد فارسی و قالب‌بندی زمان/فرکانس.
 * این آبجکت خالص است و در تست‌های واحد پوشش داده می‌شود.
 */
object PersianText {

    private val arabicToPersian = mapOf(
        'ي' to 'ی',
        'ك' to 'ک',
        'ۀ' to 'ه',
        'ة' to 'ه',
        'أ' to 'ا',
        'إ' to 'ا',
        'ٱ' to 'ا',
        'ؤ' to 'و',
    )

    /**
     * یکسان‌سازی حروف عربی/فارسی، حذف نیم‌فاصله (U+200C)، نشانه‌های جهت متن
     * و کشیده (tatweel) و فاصله‌های اضافی؛ خروجی برای جست‌وجوی سازگار استفاده می‌شود.
     */
    fun normalize(input: String): String {
        if (input.isEmpty()) return ""
        return buildString {
            for (ch in input) {
                when {
                    ch == '‌' || ch == '‎' || ch == '‏' || ch == '﻿' -> Unit
                    ch == 'ـ' -> Unit
                    else -> append(arabicToPersian[ch] ?: ch)
                }
            }
        }.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    }

    fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return buildString(input.length) {
            for (ch in input) {
                append(if (ch in '0'..'9') persianDigits[ch - '0'] else ch)
            }
        }
    }

    /** ms → «۳:۲۵» یا «۱:۰۲:۰۵» با ارقام فارسی */
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return toPersianDigits("0:00")
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        val raw = if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
        return toPersianDigits(raw)
    }

    /** فرکانس باندهای اکولایزر: ۶۰ ، ۹۶۰ ، ۳.۶k ، ۱۲k */
    fun formatFrequency(hz: Int): String {
        val raw = if (hz < 1000) {
            hz.toString()
        } else {
            val k = hz / 1000.0
            if (k >= 10.0 || k == k.toInt().toDouble()) {
                "${k.toInt()}k"
            } else {
                String.format(Locale.US, "%.1fk", k)
            }
        }
        return toPersianDigits(raw)
    }

    /** نمایش دسی‌بل با علامت: +۳ ، -۲.۵ ، ۰ */
    fun formatSignedDb(db: Float): String {
        val rounded = (db * 10).toInt() / 10.0
        val raw = when {
            rounded > 0.0 ->
                if (rounded == rounded.toInt().toDouble()) "+${rounded.toInt()}" else "+$rounded"
            rounded < 0.0 ->
                if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}" else "$rounded"
            else -> "0"
        }
        return toPersianDigits(raw)
    }
}
