# راهنمای انتشار در Google Play — SoundPilot

این سند چک‌لیست کامل و متن‌های آماده برای انتشار جهانی است.

## ۱. پیش‌نیازهای فنی — وضعیت فعلی ✅

| الزام | وضعیت |
|---|---|
| targetSdk 36 (الزام اپ‌های جدید از ۳۱ اوت ۲۰۲۶) | ✅ |
| minSdk 26 (اندروید ۸ به بالا — پوشش >۹۸٪ گوشی‌ها) | ✅ |
| امضای release (scheme v2/v3) با keystore اختصاصی | ✅ |
| R8/minify + shrinkResources فعال | ✅ |
| مجوزهای حداقلی (بدون INTERNET) | ✅ |
| `foregroundServiceType="mediaPlayback"` (الزام اندروید ۱۴+) | ✅ |
| `POST_NOTIFICATIONS` ران‌تایم (اندروید ۱۳+) | ✅ |
| `enableOnBackInvokedCallback` (بک پیش‌بین اندروید ۱۵+) | ✅ |
| `localeConfig` (انتخاب زبان هر اپ، اندروید ۱۳+) | ✅ |
| RTL کامل + تم تیره/روشن/سیستمی | ✅ |
| `allowBackup=false` (حریم خصوصی) | ✅ |

## ۲. فایل انتشار

Google Play **فقط** AAB می‌پذیرد:

```bash
./gradlew bundleRelease
# خروجی: app/build/outputs/bundle/release/app-release.aab
```

برای تست روی گوشی می‌توان از APK استفاده کرد:
```bash
./gradlew assembleRelease
```

⚠️ امضا باید با همان keystore انتشار باشد (`keystore.properties` — این فایل را هرگز
در گیت کامیت نکنید و از آن نسخه پشتیبان امن نگه دارید؛ بدون آن آپدیت‌های بعدی ممکن نیست).

## ۳. فرم Data Safety (پاسخ‌های دقیق)

- Does your app collect or share any of the required user data types? → **No**
- (چون مجوز اینترنت وجود ندارد و هیچ SDK ردیابی نیست، بخش جمع‌آوری خالی می‌ماند)
- Is all of the user data collected by your app encrypted in transit? → **Not applicable**
- Do you provide a way for users to request that their data is deleted? → **Not applicable**
- لینک Privacy Policy: آدرس `PRIVACY.md` در مخزن (یا هاست خودتان)

## ۴. متن فروشگاه (Store Listing)

### عنوان (۳۰ کاراکتر)
```
SoundPilot: پلیر ماشین و اکولایزر
```
`SoundPilot: Car Player & Equalizer`

### توضیح کوتاه (۸۰ کاراکتر)
```
پلیر آفلاین موسیقی برای ضبط ماشین: اکولایزر واقعی، تقویت بلندی و بیس، تایمر خواب
```

### توضیح کامل
```
SoundPilot — پلیر موسیقی حرفه‌ای آفلاین، طراحی‌شده برای پخش از گوشی به ضبط ماشین.

🔊 کیفیت صدای واقعی برای خودرو:
• اکولایزر ۵ تا ۱۰ باندی متصل به موتور صوتی دستگاه (واقعی، نه تزئینی)
• ۱۰ پروفایل آماده: خودرو، بیس‌دار، وکال، شفافیت، راک، پاپ، جاز، کلاسیک، الکترونیک
• تقویت بیس (BassBoost) و صدای فراگیر (Virtualizer)
• تقویت بلندی (Loudness) برای ضبط‌های کم‌صدا در محیط پرنویز جاده
• پرش خودکار از سکوت‌های طولانی
• تنظیم سرعت پخش با حفظ گام صدا (۰٫۵ تا ۲ برابر)

🚗 ساخته‌شده برای رانندگی:
• حالت خودرو با دکمه‌های بزرگ و حداقل حواس‌پرتی
• کنترل کامل از فرمان ماشین، صفحه قفل و بلوتوث (MediaSession)
• بازیابی خودکار آخرین پخش
• تایمر خواب (زمان‌دار یا پایان همین آهنگ)

🎵 پخش‌کننده کامل:
• کتابخانه هوشمند با جست‌وجوی سریع فارسی
• آلبوم‌ها، خواننده‌ها، علاقه‌مندی‌ها و صف پخش
• پخش تصادفی، تکرار، صف پخش قابل ویرایش
• پس‌زمینه دینامیک هم‌رنگ کاور آلبوم
• سوایپ برای تغییر آهنگ

🔒 کاملاً آفلاین و خصوصی:
• بدون اینترنت، بدون تبلیغات، بدون جمع‌آوری هیچ داده‌ای
• همه چیز روی خود دستگاه شما می‌ماند
```

### Categories & Tags
- Category: **Music & Audio**
- Tags: offline music player, equalizer, car audio

## ۵. ارزیابی محتوا و دسته‌بندی

- Content rating: **Everyone (3+)**
- Contains ads: **No**
- In-app purchases: **No**
- Data safety: مطابق بخش ۳

## ۶. چک‌لیست پیش از ارسال

- [x] تست نصب نسخه release روی گوشی واقعی (Galaxy A33 5G / Android 16 انجام شد)
- [ ] Pre-launch report در Play Console را بعد از آپلود بررسی کنید (سندباکس فقط یک دستگاه دارد)
- [ ] اسکرین‌شات: حداقل ۲ عدد (کتابخانه + صفحه پخش)، ۹:۱۶ یا ۱۶:۹، حداقل ۱۰۸۰px
- [ ] آیکون ۵۱۲×۵۱۲ PNG (بدون شفافیت) + تصویر شاخص ۱۰۲۴×۵۰۰
- [ ] پر کردن فرم App content (حریم خصوصی، تبلیغات، محتوا)
- [ ] اگر اپ internal testing می‌شود، اول track داخلی، بعد production

## ۷. امنیت

- keystore و `keystore.properties` هرگز به مخزن commit نشده‌اند (.gitignore)
- توکن‌های GitHub فقط به‌صورت موقت استفاده شده‌اند
- APK release با `apksigner verify` صحت‌سنجی شده است
- برای انتشار، فقط فایل‌های امضاشده همین مخزن را با build خودتان تولید کنید
