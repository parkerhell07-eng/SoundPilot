# حریم خصوصی — SoundPilot | Privacy Policy

> آخرین به‌روزرسانی / Last updated: 2025-10-20

---

## فارسی

### اصل اصلی: جمع‌آوری صفرِ داده

SoundPilot یک پلیر موسیقی **کاملاً آفلاین** است:

- ❌ هیچ داده‌ای جمع‌آوری نمی‌شود
- ❌ هیچ داده‌ای به هیچ سروری ارسال نمی‌شود
- ❌ هیچ SDK تبلیغاتی، آنالیتیکس یا ردیابی وجود ندارد
- ❌ برنامه **اصلاً مجوز اینترنت ندارد** — از نظر فنی هم امکان ارسال داده وجود ندارد

### مجوزها و دلیل هرکدام

| مجوز | چرا لازم است |
|---|---|
| `READ_MEDIA_AUDIO` | نمایش و پخش آهنگ‌های موجود روی خودِ دستگاه |
| `READ_EXTERNAL_STORAGE` (تا اندروید ۱۳) | همان کاربرد بالا برای نسخه‌های قدیمی‌تر اندروید |
| `POST_NOTIFICATIONS` | نمایش اعلان رسانه هنگام پخش در پس‌زمینه (کنترل صفحه قفل/هدست) |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | ادامه پخش هنگام بسته‌بودن صفحه |
| `WAKE_LOCK` | جلوگیری از قطع پخش هنگام خواب دستگاه |
| `MODIFY_AUDIO_SETTINGS` | اعمال اکولایزر و افکت‌های صوتی روی خروجی خود برنامه |

### داده‌های روی دستگاه

علاقه‌مندی‌ها، تنظیمات (اکولایزر، سرعت، تم و…) و آخرین وضعیت پخش فقط در حافظه محلی
(DataStore) نگه‌داری می‌شوند و هیچ‌وقت از دستگاه خارج نمی‌شوند. پشتیبان‌گیری خودکار
به سرویس‌های ابری اندروید نیز عمداً غیرفعال است.

### کودکان

این برنامه برای همه گروه‌های سنی است و هیچ محتوای نامناسبی ندارد (ارزیابی محتوای
Everyone / 3+).

### تماس

هر سؤالی درباره این سیاست دارید، از طریق مخزن رسمی پروژه در GitHub مطرح کنید.

---

## English

### Core principle: zero data collection

SoundPilot is a **fully offline** music player:

- ❌ No data is collected
- ❌ No data is sent to any server
- ❌ No ads, analytics, or tracking SDKs
- ❌ The app has **no INTERNET permission at all** — sending data is technically impossible

### Permissions and why each one is needed

| Permission | Purpose |
|---|---|
| `READ_MEDIA_AUDIO` | List and play music files already on the device |
| `READ_EXTERNAL_STORAGE` (≤ Android 13) | Same purpose on older Android versions |
| `POST_NOTIFICATIONS` | Show the media notification during background playback |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep playback running when the screen is off |
| `WAKE_LOCK` | Prevent playback interruption during device sleep |
| `MODIFY_AUDIO_SETTINGS` | Apply the equalizer/effects to this app's own output |

### On-device data

Favorites, settings (equalizer, speed, theme, …) and last playback state are stored
locally only (DataStore) and never leave the device. Cloud backup of app data is
intentionally disabled.

### Children

This app is suitable for all ages and contains no objectionable content
(Everyone / 3+ rating).

### Contact

For any questions about this policy, open an issue on the official GitHub repository.
