package com.soundpilot.app.update

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundpilot.app.di.ServiceLocator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/** وضعیت کامل جریان بروزرسانی برای UI */
sealed interface UpdateUi {
    /** هنوز بررسی نشده */
    data object Idle : UpdateUi

    /** بررسی در جریان است */
    data object Checking : UpdateUi

    /** به‌روز هستید */
    data class UpToDate(val version: String) : UpdateUi

    /** نسخه جدید موجود است */
    data class Available(val info: ReleaseInfo) : UpdateUi

    /** در حال دانلود */
    data class Downloading(val info: ReleaseInfo, val progress: Float) : UpdateUi

    /** دانلود کامل شد — آماده نصب */
    data class ReadyToInstall(val info: ReleaseInfo, val file: File) : UpdateUi

    /** بررسی ناموفق بود (مثلاً بدون اینترنت) */
    data class Failed(val message: String) : UpdateUi
}

/**
 * ViewModel بروزرسانی درون‌برنامه‌ای.
 *
 * - بررسی خودکار هر ۲۴ ساعت یک‌بار و فقط اگر کاربر اجازه داده باشد؛
 *   خطای بررسی خودکار هرگز به کاربر نمایش داده نمی‌شود.
 * - دانلود و نصب همیشه با فرمان صریح کاربر انجام می‌شود.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val manager: UpdateManager = ServiceLocator.updateManager
    private val settings = ServiceLocator.settingsRepository

    private val _state = MutableStateFlow<UpdateUi>(UpdateUi.Idle)
    val state: StateFlow<UpdateUi> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** بنر «نسخه جدید» پس از رد کردن، تا پایان همین اجرا دیگر نشان داده نمی‌شود */
    private val _bannerDismissed = MutableStateFlow(false)
    val bannerDismissed: StateFlow<Boolean> = _bannerDismissed.asStateFlow()

    private var downloadJob: kotlinx.coroutines.Job? = null

    private var lastDownloaded: File? = null

    init {
        viewModelScope.launch { autoCheckIfNeeded() }
    }

    fun dismissBanner() {
        _bannerDismissed.value = true
    }

    /** بررسی دستی با بازخورد کامل به کاربر */
    fun checkNow() {
        if (_state.value is UpdateUi.Checking) return
        viewModelScope.launch {
            _state.value = UpdateUi.Checking
            try {
                val info = manager.fetchLatestRelease()
                settings.markUpdateCheckedAt()
                val current = currentVersion()
                _state.value = if (UpdateCompat.isNewer(info.version, current)) {
                    UpdateUi.Available(info)
                } else {
                    UpdateUi.UpToDate(current)
                }
            } catch (e: Exception) {
                Log.w(TAG, "بررسی بروزرسانی ناموفق بود", e)
                _state.value = UpdateUi.Failed(friendlyMessage(e))
            }
        }
    }

    /** دانلود نسخه جدید با نمایش پیشرفت */
    fun download(info: ReleaseInfo) {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            _state.value = UpdateUi.Downloading(info, 0f)
            try {
                val file = manager.downloadApk(info) { progress ->
                    if (_state.value is UpdateUi.Downloading) {
                        _state.value = UpdateUi.Downloading(info, progress)
                    }
                }
                lastDownloaded = file
                _state.value = UpdateUi.ReadyToInstall(info, file)
            } catch (e: Exception) {
                Log.w(TAG, "دانلود بروزرسانی ناموفق بود", e)
                _state.value = UpdateUi.Available(info)
                _events.tryEmit("دانلود ناموفق بود؛ اتصال اینترنت را بررسی کنید")
            }
        }
    }

    /**
     * نصب فایل دانلودشده. اگر اجازه نصب داده نشده باشد، صفحه اجازه باز می‌شود
     * و پیام راهنما منتشر می‌گردد.
     */
    fun install(info: ReleaseInfo) {
        val file = lastDownloaded ?: manager.downloadedApk()
        if (file == null || !file.exists()) {
            _state.value = UpdateUi.Available(info)
            _events.tryEmit("فایل دانلودشده پیدا نشد؛ دوباره دانلود کنید")
            return
        }
        val ok = manager.launchInstall(file)
        if (!ok) {
            _events.tryEmit("پس از دادن اجازه نصب، دوباره روی «نصب» بزنید")
        }
    }

    /** نسخه نصب‌شده فعلی از PackageManager — همیشه با بیلد هم‌خوان است */
    fun currentVersion(): String = runCatching {
        getApplication<Application>().packageManager
            .getPackageInfo(getApplication<Application>().packageName, 0)
            .versionName
            .orEmpty()
    }.getOrDefault("")

    /** بررسی خودکار کم‌سروصدا: هر ۲۴ ساعت و فقط با اجازه کاربر */
    private suspend fun autoCheckIfNeeded() {
        runCatching {
            if (!settings.updateAutoCheck.first()) return
            val last = settings.lastUpdateCheckAt.first()
            val now = System.currentTimeMillis()
            if (now - last < AUTO_CHECK_INTERVAL_MS) return
            val info = manager.fetchLatestRelease()
            settings.markUpdateCheckedAt()
            if (UpdateCompat.isNewer(info.version, currentVersion())) {
                _state.value = UpdateUi.Available(info)
            } else {
                _state.value = UpdateUi.UpToDate(currentVersion())
            }
        }.onFailure {
            // بررسی خودکار هرگز مزاحم کاربر نمی‌شود
            Log.d(TAG, "بررسی خودکار بروزرسانی انجام نشد (طبیعی بدون اینترنت)", it)
        }
    }

    private fun friendlyMessage(e: Exception): String = when (e) {
        is IOException -> "بررسی ناموفق بود؛ اتصال اینترنت را بررسی کنید"
        else -> "بررسی بروزرسانی ناموفق بود؛ کمی بعد دوباره تلاش کنید"
    }

    companion object {
        private const val TAG = "UpdateViewModel"
        const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
