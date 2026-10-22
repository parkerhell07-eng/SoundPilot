# حریم خصوصی — SoundPilot | Privacy Policy

> آخرین به‌روزرسانی / Last updated: 2026-10-22

---

## فارسی

### اصل اصلی: جمع‌آوری صفرِ داده

SoundPilot یک پلیر موسیقی آفلاین است و **هیچ داده‌ای جمع‌آوری نمی‌کند**:

- ❌ هیچ داده‌ای جمع‌آوری نمی‌شود
- ❌ هیچ داده‌ای به هیچ سروری ارسال نمی‌شود
- ❌ هیچ SDK تبلیغاتی، آنالیتیکس یا ردیابی وجود ندارد

### تنها استفاده از اینترنت: بروزرسانی درون‌برنامه‌ای (نسخه ۱.۴+)

از نسخه ۱.۴، برنامه مجوز `INTERNET` دارد، اما فقط برای **یک کار مشخص**:

- بررسی نسخه جدید از مخزن رسمی گیت‌هاب (`api.github.com`) — بدون ارسال هیچ داده‌ای؛
  این درخواست از نظر فنی مثل بازکردن یک صفحه وب در مرورگر است و شناسه یا اطلاعات
  کاربری در آن وجود ندارد.
- دانلود فایل نصب نسخه جدید از گیت‌هاب — **فقط با فرمان صریح کاربر**.
- بررسی خودکار هر ۲۴ ساعت یک‌بار انجام می‌شود و در تنظیمات «بروزرسانی» قابل خاموش‌کردن است.
- هیچ دانلود، تبلیغ یا درخواست شبکه دیگری در هیچ بخشی از برنامه وجود ندارد.

### مجوزها و دلیل هرکدام

| مجوز | چرا لازم است |
|---|---|
| `READ_MEDIA_AUDIO` | نمایش و پخش آهنگ‌های موجود روی خودِ دستگاه |
| `READ_EXTERNAL_STORAGE` (تا اندروید ۱۳) | همان کاربرد بالا برای نسخه‌های قدیمی‌تر اندروید |
| `POST_NOTIFICATIONS` | نمایش اعلان رسانه هنگام پخش در پس‌زمینه (کنترل صفحه قفل/هدست) |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | ادامه پخش هنگام بسته‌بودن صفحه |
| `WAKE_LOCK` | جلوگیری از قطع پخش هنگام خواب دستگاه |
| `MODIFY_AUDIO_SETTINGS` | اعمال اکولایزر و افکت‌های صوتی روی خروجی خود برنامه |
| `INTERNET` | فقط برای بروزرسانی درون‌برنامه‌ای از گیت‌هاب (بالا توضیح داده شد) |
| `REQUEST_INSTALL_PACKAGES` | برای نصب نسخه جدید پس از دانلود — همیشه با تأیید صریح کاربر |

### داده‌های روی دستگاه

علاقه‌مندی‌ها، آمار پخش، تنظیمات (اکولایزر، سرعت، تم و…) و آخرین وضعیت پخش فقط در
حافظه محلی (DataStore) نگه‌داری می‌شوند و هیچ‌وقت از دستگاه خارج نمی‌شوند. پشتیبان‌گیری
خودکار به سرویس‌های ابری اندروید نیز عمداً غیرفعال است.

### کودکان

این برنامه برای همه گروه‌های سنی است و هیچ محتوای نامناسبی ندارد (ارزیابی محتوای
Everyone / 3+).

### تماس

هر سؤالی درباره این سیاست دارید، از طریق مخزن رسمی پروژه در GitHub مطرح کنید.

---

## English

### Core principle: zero data collection

SoundPilot is an offline music player that **collects no data**:

- ❌ No data is collected
- ❌ No data is sent to any server
- ❌ No ads, analytics, or tracking SDKs

### The only network use: in-app updates (v1.4+)

Starting with version 1.4, the app holds the `INTERNET` permission, used **exclusively** for:

- Checking for a newer release against the official GitHub repository (`api.github.com`) —
  no data is transmitted; this is equivalent to loading a web page in a browser.
- Downloading the new APK from GitHub — **only when the user explicitly taps “Download”**.
- Auto-check runs at most once every 24 hours and can be disabled in Settings → Update.
- No other downloads, ads, or network requests exist anywhere in the app.

### Permissions and why each one is needed

| Permission | Purpose |
|---|---|
| `READ_MEDIA_AUDIO` | List and play music files already on the device |
| `READ_EXTERNAL_STORAGE` (≤ Android 13) | Same purpose on older Android versions |
| `POST_NOTIFICATIONS` | Show the media notification during background playback |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep playback running when the screen is off |
| `WAKE_LOCK` | Prevent playback interruption during device sleep |
| `MODIFY_AUDIO_SETTINGS` | Apply the equalizer/effects to this app's own output |
| `INTERNET` | In-app updates from GitHub only (explained above) |
| `REQUEST_INSTALL_PACKAGES` | Install the downloaded update — always with explicit user confirmation |

### On-device data

Favorites, play statistics, settings (equalizer, speed, theme, …) and last playback
state are stored locally only (DataStore) and never leave the device. Cloud backup
of app data is intentionally disabled.

### Children

This app is suitable for all ages and contains no objectionable content
(Everyone / 3+ rating).

### Contact

For any questions about this policy, open an issue on the official GitHub repository.
