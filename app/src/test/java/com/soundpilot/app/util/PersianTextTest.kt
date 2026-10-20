package com.soundpilot.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PersianTextTest {

    @Test
    fun `normalize unifies Arabic letters with Persian equivalents`() {
        assertEquals("کلمه", PersianText.normalize("كلمه"))
        assertEquals("یاس", PersianText.normalize("ياس"))
        assertEquals("هه", PersianText.normalize("ۀة"))
        assertEquals("ابوابکر", PersianText.normalize("أبوإبکر"))
    }

    @Test
    fun `normalize removes zwnj and direction marks and collapses spaces`() {
        assertEquals("میروم خانه", PersianText.normalize("می‌روم   خانه"))
        assertEquals("ab", PersianText.normalize("a\u200Fb"))
        assertEquals("a b", PersianText.normalize("  a\t\nb  "))
    }

    @Test
    fun `normalize lowercases latin letters`() {
        assertEquals("hello", PersianText.normalize("HeLLo"))
    }

    @Test
    fun `toPersianDigits converts all latin digits`() {
        assertEquals("۰۱۲۳۴۵۶۷۸۹", PersianText.toPersianDigits("0123456789"))
        assertEquals("آهنگ ۱۲", PersianText.toPersianDigits("آهنگ 12"))
    }

    @Test
    fun `formatDuration renders m ss and h mm ss in persian digits`() {
        assertEquals("۰:۰۰", PersianText.formatDuration(0))
        assertEquals("۱:۱۵", PersianText.formatDuration(75_000))
        assertEquals("۳:۲۵", PersianText.formatDuration(205_000))
        assertEquals("۱:۰۲:۰۵", PersianText.formatDuration(3_725_000))
    }

    @Test
    fun `formatFrequency renders compact labels`() {
        assertEquals("۶۰", PersianText.formatFrequency(60))
        assertEquals("۲۳۰", PersianText.formatFrequency(230))
        assertEquals("۱k", PersianText.formatFrequency(1000))
        assertEquals("۳.۶k", PersianText.formatFrequency(3600))
        assertEquals("۱۲k", PersianText.formatFrequency(12_000))
    }

    @Test
    fun `formatSignedDb shows sign and one decimal at most`() {
        assertEquals("+۳", PersianText.formatSignedDb(3f))
        assertEquals("+۲.۵", PersianText.formatSignedDb(2.5f))
        assertEquals("-۱", PersianText.formatSignedDb(-1f))
        assertEquals("-۲.۵", PersianText.formatSignedDb(-2.5f))
        assertEquals("۰", PersianText.formatSignedDb(0f))
    }
}
