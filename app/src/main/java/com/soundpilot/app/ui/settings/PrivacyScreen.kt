package com.soundpilot.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * سیاست حریم خصوصی درون‌برنامه‌ای — مطابق الزامات Google Play برای شفافیت داده.
 * محتوای کامل در فایل PRIVACY.md مخزن هم نگهداری می‌شود.
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "بازگشت",
                )
            }
            Text(
                text = "حریم خصوصی و امنیت",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))

        PrivacyCard(
            icon = Icons.Rounded.Block,
            title = "جمع‌آوری داده: صفر",
            body = "SoundPilot هیچ داده‌ای جمع‌آوری، ذخیره‌سازی ابری یا ارسال نمی‌کند. " +
                "هیچ SDK ردیابی، تبلیغات یا آنالیتیکسی در برنامه وجود ندارد.",
        )
        Spacer(Modifier.height(12.dp))
        PrivacyCard(
            icon = Icons.Rounded.CloudDownload,
            title = "تنها ارتباط شبکه‌ای: بروزرسانی",
            body = "تنها استفاده از اینترنت، بررسی نسخه جدید از رسمی مخزن گیت‌هاب است. " +
                "در این درخواست هیچ داده‌ای از شما ارسال نمی‌شود (فقط یک درخواست ساده مثل بازکردن یک صفحه وب). " +
                "بررسی خودکار هر ۲۴ ساعت یک‌بار انجام می‌شود و می‌توانید آن را در تنظیمات خاموش کنید؛ " +
                "دانلود و نصب همیشه فقط با فرمان صریح شما انجام می‌شود.",
        )
        Spacer(Modifier.height(12.dp))
        PrivacyCard(
            icon = Icons.Rounded.MusicNote,
            title = "دسترسی به فایل‌های صوتی",
            body = "مجوز «موسیقی و صدا» (READ_MEDIA_AUDIO) فقط برای خواندن فهرست آهنگ‌های " +
                "خودِ دستگاه و پخش آن‌ها استفاده می‌شود. هیچ فایلی خوانده، ویرایش، حذف یا " +
                "جابه‌جا نمی‌شود و محتوای کتابخانه شما هرگز از دستگاه خارج نمی‌شود.",
        )
        Spacer(Modifier.height(12.dp))
        PrivacyCard(
            icon = Icons.Rounded.NotificationsActive,
            title = "اعلان‌ها",
            body = "مجوز اعلان فقط برای نمایش اعلان رسانه هنگام پخش پس‌زمینه (با کنترل صفحه قفل " +
                "و هدست) استفاده می‌شود. هیچ اعلان تبلیغاتی یا بازاریابی ارسال نمی‌شود.",
        )
        Spacer(Modifier.height(12.dp))
        PrivacyCard(
            icon = Icons.Rounded.Tune,
            title = "افکت‌های صوتی",
            body = "مجوز MODIFY_AUDIO_SETTINGS فقط برای اعمال اکولایزر و افکت‌ها روی خروجی " +
                "خودِ SoundPilot استفاده می‌شود؛ تنظیمات صدای سایر برنامه‌ها دست‌نخورده می‌ماند.",
        )
        Spacer(Modifier.height(12.dp))
        PrivacyCard(
            icon = Icons.Rounded.FolderZip,
            title = "ذخیره‌سازی محلی",
            body = "علاقه‌مندی‌ها، آمار پخش، تنظیمات و آخرین وضعیت پخش فقط در حافظه خود دستگاه (DataStore) " +
                "ذخیره می‌شود. پشتیبان‌گیری خودکار به سرویس‌های ابری اندروید غیرفعال است تا " +
                "هیچ داده‌ای بدون اطلاع شما جابه‌جا نشود.",
        )
        Spacer(Modifier.height(12.dp))
        PrivacyCard(
            icon = Icons.Rounded.EnhancedEncryption,
            title = "امنیت",
            body = "APK نسخه انتشار با امضای رمزنگاری‌شده (v2/v3) ارائه می‌شود؛ برای اطمینان " +
                "از اصالت، فقط از منابع رسمی (GitHub رسمی یا Google Play) نصب کنید. " +
                "برنامه برای دستگاه‌های Android 8 (API 26) تا Android 16 (API 36) تست و تنظیم شده است.",
        )
        Spacer(Modifier.height(12.dp))
        PrivacyCard(
            icon = Icons.Rounded.Verified,
            title = "مطابقت با قوانین",
            body = "این برنامه با سیاست‌های برنامه Google Play (هدف‌گیری targetSdk 36، " +
                "مجوزهای حداقلی، شفافیت داده) سازگار است. اگر در هر زمان نکته‌ای درباره " +
                "حریم خصوصی داشتید، از صفحه مخزن رسمی پروژه با ما در میان بگذارید.",
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PrivacyCard(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
