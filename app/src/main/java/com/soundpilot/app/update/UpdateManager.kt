package com.soundpilot.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * مدیریت بروزرسانی درون‌برنامه‌ای از GitHub Releases.
 *
 * تنها ارتباط شبکه‌ای کل برنامه همین‌جاست:
 * - GET از api.github.com برای بررسی نسخه جدید (بدون ارسال هیچ داده‌ای)
 * - دانلود فایل APK نسخه جدید از GitHub (فقط با فرمان کاربر)
 *
 * نصب با استاندارد اندروید انجام می‌شود: فایل از طریق FileProvider به
 * نصاب سیستم داده می‌شود؛ هیچ نصب خاموش یا دورزدن امنیتی وجود ندارد.
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val RELEASES_URL =
            "https://api.github.com/repos/parkerhell07-eng/SoundPilot/releases/latest"
        private const val CONNECT_TIMEOUT_MS = 12_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val DOWNLOAD_READ_TIMEOUT_MS = 60_000
        private const val BUFFER_SIZE = 16 * 1024
    }

    // ---------------- بررسی نسخه ----------------

    /** دریافت آخرین Release منتشرشده از GitHub API */
    suspend fun fetchLatestRelease(): ReleaseInfo = withContext(Dispatchers.IO) {
        val conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "SoundPilot-Updater")
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("پاسخ نامعتبر گیت‌هاب (کد $code)")
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            UpdateCompat.parseRelease(body)
                ?: throw IOException("نسخه جدید قابل نصبی در گیت‌هاب پیدا نشد")
        } finally {
            conn.disconnect()
        }
    }

    // ---------------- دانلود ----------------

    /** پوشه دانلود بروزرسانی — ترجیحاً حافظه خارجی اختصاصی برنامه */
    fun updateDir(): File {
        val dir = context.getExternalFilesDir("updates")
            ?: File(context.cacheDir, "updates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** آخرین APK دانلودشده (اگر باشد) — برای ازسرگیری پس از بستن برنامه */
    fun downloadedApk(): File? =
        updateDir().listFiles()
            ?.filter { it.isFile && it.name.endsWith(".apk") }
            ?.maxByOrNull { it.lastModified() }

    /**
     * دانلود APK نسخه جدید با گزارش درصد پیشرفت.
     * پوشه همیشه تمیز نگه داشته می‌شود: نسخه‌های قبلی حذف می‌شوند.
     */
    suspend fun downloadApk(info: ReleaseInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = updateDir()
            dir.listFiles()?.forEach { runCatching { it.delete() } }
            val tmp = File(dir, "${info.apkName}.part")
            val out = File(dir, info.apkName)

            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "SoundPilot-Updater")
            }
            try {
                if (conn.responseCode !in 200..299) {
                    throw IOException("دانلود ناموفق بود (کد ${conn.responseCode})")
                }
                val total = (conn.contentLengthLong.takeIf { it > 0 } ?: info.apkSizeBytes)
                    .coerceAtLeast(1L)
                conn.inputStream.use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read: Int
                        var written = 0L
                        var lastPercent = -1
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            written += read
                            val percent = ((written * 100) / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent / 100f)
                            }
                        }
                        output.flush()
                    }
                }
                if (info.apkSizeBytes > 0 && tmp.length() != info.apkSizeBytes) {
                    throw IOException("حجم فایل دانلودشده با نسخه رسمی یکی نیست")
                }
                if (!tmp.renameTo(out)) {
                    throw IOException("ذخیره نهایی فایل ناموفق بود")
                }
                out
            } catch (e: Exception) {
                runCatching { tmp.delete() }
                throw e
            } finally {
                conn.disconnect()
            }
        }

    // ---------------- نصب ----------------

    /** آیا اجازه نصب بسته‌های ناشناس (منبع گیت‌هاب) داده شده است؟ */
    fun canRequestInstall(): Boolean = runCatching {
        context.packageManager.canRequestPackageInstalls()
    }.getOrDefault(false)

    /** بازکردن صفحه اجازه نصب برای همین برنامه */
    fun openUnknownSourcesSettings(): Boolean = runCatching {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    /**
     * راه‌اندازی نصاب سیستم برای فایل دانلودشده.
     * اگر اجازه نصب داده نشده باشد، صفحه اجازه باز می‌شود و false برمی‌گردد
     * (کاربر پس از اجازه، دوباره دکمه نصب را می‌زند).
     */
    fun launchInstall(file: File): Boolean {
        if (!file.exists()) return false
        if (!canRequestInstall()) {
            openUnknownSourcesSettings()
            return false
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
