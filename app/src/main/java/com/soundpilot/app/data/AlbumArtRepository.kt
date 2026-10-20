package com.soundpilot.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * پایپ‌لاین واقعی تصویر آلبوم برای اندروید مدرن.
 *
 * URI قدیمی «content://media/external/audio/albumart/<id>» از اندروید ۱۰ به بعد
 * غیرقابل‌اعتماد/ازکارافتاده است؛ راه استاندارد امروز:
 *  - API 29+: ContentResolver.loadThumbnail روی URI خود فایل صوتی (کاور تعبیه‌شده)
 *  - API 26-28: MediaMetadataRetriever.embeddedPicture
 *
 * خروجی در cacheDir/artwork ذخیره (کش دیسک) و در حافظه (LruCache) نگه‌داری می‌شود.
 * همه‌چیز روی Dispatchers.IO و با deduplication درخواست‌های همزمان.
 */
object AlbumArtRepository {

    private const val TAG = "AlbumArt"
    private const val THUMB_SIZE = 512

    private lateinit var appContext: Context

    /** albumId → file:// uri ؛ فقط نتیجه‌های موفق */
    private val memoryCache = LruCache<Long, String>(256)

    private val diskMutex = Mutex()
    private val inFlight = HashMap<Long, kotlinx.coroutines.Deferred<String?>>()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
    }

    private val artworkDir: File
        get() = File(appContext.cacheDir, "artwork").apply { mkdirs() }

    /** تنها مقدار کش‌شده (بدون I/O) — برای ساخت MediaItem بدون مسدودشدن ترد */
    fun cachedUri(albumId: Long): String? =
        if (albumId <= 0) null else memoryCache.get(albumId)

    /**
     * واکشی آرت‌ورک با کش دو سطحی. هرگز اپلیکیشن را کرش نمی‌دهد؛
     * در نبود کاور null برمی‌گرداند.
     */
    suspend fun uriFor(albumId: Long, songId: Long): String? {
        if (albumId <= 0) return null
        memoryCache.get(albumId)?.let { return it }

        // dedup درخواست‌های همزمان برای یک آلبوم
        val deferred = synchronized(inFlight) {
            inFlight.getOrPut(albumId) {
                ioScope.async { resolveAndCache(albumId, songId) }
            }
        }
        return try {
            deferred.await()
        } finally {
            synchronized(inFlight) { inFlight.remove(albumId, deferred) }
        }
    }

    /** گرم‌کردن بی‌صدا در پس‌زمینه (مثلاً برای ردیف‌های قابل مشاهده) */
    fun warmUp(albumId: Long, songId: Long) {
        if (albumId <= 0) return
        if (memoryCache.get(albumId) != null) return
        ioScope.launch { runCatching { uriFor(albumId, songId) } }
    }

    private suspend fun resolveAndCache(albumId: Long, songId: Long): String? =
        diskMutex.withLock {
            memoryCache.get(albumId)?.let { return it }

            val cacheFile = File(artworkDir, "album_$albumId.jpg")
            if (cacheFile.exists() && cacheFile.length() > 0) {
                val uri = Uri.fromFile(cacheFile).toString()
                memoryCache.put(albumId, uri)
                return uri
            }

            val bitmap = withContext(Dispatchers.IO) {
                loadBitmap(albumId, songId)
            } ?: return null

            try {
                cacheFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                val uri = Uri.fromFile(cacheFile).toString()
                memoryCache.put(albumId, uri)
                uri
            } catch (e: Exception) {
                Log.w(TAG, "ذخیره کاور در کش ناموفق بود", e)
                null
            } finally {
                bitmap.recycle()
            }
        }

    private fun loadBitmap(albumId: Long, songId: Long): Bitmap? {
        // ۱) مسیر استاندارد اندروید ۱۰+: thumbnail خود فایل صوتی
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val songUri = Uri.parse("content://media/external/audio/media/$songId")
                val bmp = appContext.contentResolver.loadThumbnail(
                    songUri,
                    android.util.Size(THUMB_SIZE, THUMB_SIZE),
                    null,
                )
                if (bmp != null && bmp.width > 1) return bmp
                runCatching { bmp?.recycle() }
            } catch (e: Exception) {
                Log.d(TAG, "loadThumbnail برای آلبوم $albumId نتیجه نداشت", e)
            }
        }

        // ۲) مسیر جایگزین: کاور تعبیه‌شده در خود فایل
        if (songId > 0) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                retriever.setDataSource(
                    appContext,
                    Uri.parse("content://media/external/audio/media/$songId"),
                )
                val art = retriever.embeddedPicture
                if (art != null) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                    val bmp = BitmapFactory.decodeByteArray(art, 0, art.size, opts)
                    if (bmp != null && bmp.width > THUMB_SIZE * 2) {
                        val scaled = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true)
                        if (scaled != bmp) bmp.recycle()
                        return scaled
                    }
                    return bmp
                }
            } catch (e: Exception) {
                Log.d(TAG, "کاور تعبیه‌شده برای آلبوم $albumId یافت نشد", e)
            } finally {
                runCatching { retriever?.release() }
            }
        }

        // ۳) آخرین تلاش: کوئری مستقیم آلبوم (برای برخی دستگاه‌ها جواب می‌دهد)
        try {
            val albumUri = Uri.parse("content://media/external/audio/albumart/$albumId")
            val bmp = appContext.contentResolver.loadThumbnail(
                albumUri,
                android.util.Size(THUMB_SIZE, THUMB_SIZE),
                null,
            )
            if (bmp != null && bmp.width > 1) return bmp
        } catch (_: Exception) {
            // آرت‌ورک واقعاً وجود ندارد — کاملاً طبیعی است
        }

        return null
    }

    /** پاک‌سازی کش (برای تنظیمات/اشکال‌زدایی) */
    fun clearCache() {
        memoryCache.evictAll()
        ioScope.launch {
            runCatching { artworkDir.deleteRecursively() }
        }
    }
}
