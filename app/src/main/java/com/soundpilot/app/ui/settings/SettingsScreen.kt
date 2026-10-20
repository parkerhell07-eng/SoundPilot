package com.soundpilot.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundpilot.app.data.SettingsRepository
import com.soundpilot.app.data.ThemeMode
import com.soundpilot.app.di.ServiceLocator
import com.soundpilot.app.util.PersianText
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()
    val settings = ServiceLocator.settingsRepository

    val themeMode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.DARK)
    val carMode by settings.carMode.collectAsStateWithLifecycle(false)
    val restoreLast by settings.restoreLastPlayed.collectAsStateWithLifecycle(true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "تنظیمات",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))

        // ---------- ظاهر ----------
        Text(text = "ظاهر", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { scope.launch { settings.setThemeMode(mode) } },
                    label = { Text(themeLabel(mode)) },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------- پخش ----------
        Text(text = "پخش", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        SettingSwitch(
            title = "حالت خودرو",
            subtitle = "دکمه‌های بزرگ و ساده در صفحه پخش برای استفاده ایمن هنگام رانندگی",
            checked = carMode,
            onCheckedChange = { scope.launch { settings.setCarMode(it) } },
        )
        SettingSwitch(
            title = "ادامه پخش پس از باز شدن برنامه",
            subtitle = "آخرین صف پخش و محل توقف بازیابی می‌شود؛ پخش به‌صورت خودکار آغاز نمی‌شود",
            checked = restoreLast,
            onCheckedChange = { scope.launch { settings.setRestoreLastPlayed(it) } },
        )

        Spacer(Modifier.height(20.dp))

        // ---------- درباره ----------
        Text(text = "درباره", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SoundPilot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "نسخه ${PersianText.toPersianDigits("1.0.0")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "پلیر موسیقی آفلاین با اکولایزر واقعی، طراحی‌شده برای پخش از گوشی به ضبط ماشین از طریق بلوتوث.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "حریم خصوصی",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "این برنامه کاملاً آفلاین است؛ هیچ داده‌ای جمع‌آوری یا ارسال نمی‌شود. " +
                        "آهنگ‌ها، علاقه‌مندی‌ها و تنظیمات فقط روی خود دستگاه ذخیره می‌مانند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.DARK -> "تیره"
    ThemeMode.LIGHT -> "روشن"
    ThemeMode.SYSTEM -> "سیستمی"
}
