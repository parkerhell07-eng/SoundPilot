package com.soundpilot.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soundpilot.app.data.SettingsRepository
import com.soundpilot.app.data.ThemeMode
import com.soundpilot.app.di.ServiceLocator
import com.soundpilot.app.ui.player.PlaybackViewModel
import com.soundpilot.app.util.PersianText
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    playbackVm: PlaybackViewModel,
    onOpenPrivacy: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val settings = ServiceLocator.settingsRepository

    val themeMode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.DARK)
    val carMode by settings.carMode.collectAsStateWithLifecycle(false)
    val restoreLast by settings.restoreLastPlayed.collectAsStateWithLifecycle(true)
    val skipSilence by playbackVm.skipSilence.collectAsStateWithLifecycle(false)
    val speed by playbackVm.playbackSpeed.collectAsStateWithLifecycle(1f)
    val sleepTimer by playbackVm.sleepTimerState.collectAsStateWithLifecycle()

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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        SettingSwitch(
            title = "پرش از سکوت‌های طولانی",
            subtitle = "سکوت‌های طولانی بین بخش‌های آهنگ خودکار حذف می‌شود؛ مناسب شنیدن پیوسته در خودرو",
            checked = skipSilence,
            onCheckedChange = playbackVm::setSkipSilence,
        )

        Spacer(Modifier.height(12.dp))
        SpeedRow(
            speed = speed,
            onChange = playbackVm::setPlaybackSpeed,
        )

        Spacer(Modifier.height(20.dp))

        // ---------- تایمر خواب ----------
        Text(text = "تایمر خواب", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        SleepTimerSection(
            state = sleepTimer,
            onStartMinutes = { minutes ->
                playbackVm.startSleepTimer(minutes * 60_000L)
            },
            onEndOfTrack = playbackVm::setSleepAtEndOfTrack,
            onCancel = playbackVm::cancelSleepTimer,
        )

        Spacer(Modifier.height(20.dp))

        // ---------- حریم خصوصی و امنیت ----------
        Text(text = "حریم خصوصی و امنیت", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenPrivacy,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "سیاست حریم خصوصی",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "جمع‌آوری صفر داده؛ گزارش کامل مجوزها و امنیت",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Rounded.NavigateNext,
                    contentDescription = "نمایش سیاست حریم خصوصی",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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
                            text = "نسخه ${PersianText.toPersianDigits(appVersionName())}",
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
private fun SpeedRow(
    speed: Float,
    onChange: (Float) -> Unit,
) {
    // مقدار محلی برای نمایش روان اسلایدر؛ روی رهاسازی اعمال می‌شود
    var dragValue by remember(speed) { mutableFloatStateOf(speed) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "سرعت پخش (با حفظ گام صدا)", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = PersianText.toPersianDigits(String.format(Locale.US, "%.1f×", dragValue)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        androidx.compose.material3.Slider(
            value = dragValue,
            onValueChange = { dragValue = (it * 10).roundToInt() / 10f },
            onValueChangeFinished = { onChange(dragValue) },
            valueRange = SettingsRepository.MIN_PLAYBACK_SPEED..SettingsRepository.MAX_PLAYBACK_SPEED,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SleepTimerSection(
    state: com.soundpilot.app.domain.SleepTimer.State,
    onStartMinutes: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
) {
    val active = state.mode != com.soundpilot.app.domain.SleepTimer.Mode.OFF
    val isCountdown = state.mode == com.soundpilot.app.domain.SleepTimer.Mode.COUNTDOWN

    if (active) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isCountdown) {
                            val minutes = (state.remainingMs / 60_000L).coerceAtLeast(0)
                            val seconds = (state.remainingMs % 60_000L) / 1_000L
                            "خواب پس از ${PersianText.toPersianDigits(String.format(Locale.US, "%02d:%02d", minutes, seconds))}"
                        } else {
                            "خواب در پایان همین آهنگ"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "پس از زمان مشخص‌شده، پخش متوقف می‌شود",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
                androidx.compose.material3.TextButton(onClick = onCancel) {
                    Text("لغو", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15, 30, 45, 60).forEach { minutes ->
            FilterChip(
                selected = false,
                onClick = { onStartMinutes(minutes) },
                label = {
                    Text(PersianText.toPersianDigits("$minutes دقیقه"))
                },
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    FilterChip(
        selected = false,
        onClick = onEndOfTrack,
        label = { Text("پایان همین آهنگ") },
    )
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

/** نام نسخه از PackageInfo — همیشه با versionName بیلد هم‌خوان است */
@Composable
private fun appVersionName(): String {
    val context = LocalContext.current
    return remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.2.0"
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.DARK -> "تیره"
    ThemeMode.LIGHT -> "روشن"
    ThemeMode.SYSTEM -> "سیستمی"
}
