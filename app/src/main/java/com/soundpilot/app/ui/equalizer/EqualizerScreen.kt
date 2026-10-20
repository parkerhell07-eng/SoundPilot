package com.soundpilot.app.ui.equalizer

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soundpilot.app.audio.EqualizerMath
import com.soundpilot.app.audio.EqualizerPresets
import com.soundpilot.app.ui.components.VerticalFader
import com.soundpilot.app.util.PersianText
import kotlin.math.roundToInt

/**
 * صفحه اکولایزر واقعی — متصل به نشست صوتی پلیر.
 * باندها و بازه‌ها از خود دستگاه خوانده می‌شود؛ تنظیمات همیشه ذخیره و بازیابی می‌شود.
 */
@Composable
fun EqualizerScreen() {
    val vm: EqualizerViewModel = viewModel()
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "اکولایزر",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        ui.supported && ui.sessionActive && ui.enabled -> "فعال — متصل به نشست پخش"
                        ui.supported && ui.sessionActive -> "متصل به نشست پخش (خاموش)"
                        ui.supported -> "پخش را شروع کنید تا فعال شود"
                        else -> "در انتظار تشخیص قابلیت‌های دستگاه"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = ui.enabled,
                onCheckedChange = vm::setEnabled,
                enabled = ui.sessionActive,
            )
        }

        Spacer(Modifier.height(16.dp))

        // پروفایل‌های آماده
        Text(
            text = "پروفایل‌ها",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EqualizerPresets.all.forEach { preset ->
                FilterChip(
                    selected = ui.profileId == preset.id,
                    onClick = { vm.setProfile(preset.id) },
                    label = { Text(preset.label) },
                )
            }
            FilterChip(
                selected = ui.profileId == EqualizerPresets.CUSTOM_ID,
                onClick = {},
                enabled = false,
                label = { Text("سفارشی") },
            )
        }

        Spacer(Modifier.height(16.dp))

        when {
            !ui.sessionActive -> {
                InfoCard(
                    title = "برای فعال‌سازی اکولایزر، یک آهنگ پخش کنید",
                    body = "باندهای واقعی دستگاه پس از شروع پخش شناسایی می‌شوند و " +
                        "تنظیمات شما به‌طور خودکار اعمال و ذخیره می‌شود.",
                )
            }
            !ui.supported -> {
                InfoCard(
                    title = "اکولایزر روی این دستگاه در دسترس نیست",
                    body = "موتور صوتی دستگاه اجازه اعمال افکت را نمی‌دهد؛ " +
                        "پخش بدون تغییر و بدون خطا ادامه خواهد داشت.",
                )
            }
            else -> BandsPanel(ui = ui, onBandChange = vm::setBandGain)
        }

        if (ui.sessionActive && ui.supported) {
            Spacer(Modifier.height(20.dp))

            EffectSliderRow(
                title = "تقویت بیس (BassBoost)",
                supported = ui.bassSupported,
                value = ui.bassStrength / 1000f,
                onValueChange = { vm.setBassStrength((it * 1000).roundToInt()) },
                valueText = PersianText.toPersianDigits(
                    ((ui.bassStrength / 10f).roundToInt()).toString(),
                ) + "٪",
            )
            Spacer(Modifier.height(10.dp))
            EffectSliderRow(
                title = "صدای فراگیر (Virtualizer)",
                supported = ui.virtualizerSupported,
                value = ui.virtualizerStrength / 1000f,
                onValueChange = { vm.setVirtualizerStrength((it * 1000).roundToInt()) },
                valueText = PersianText.toPersianDigits(
                    ((ui.virtualizerStrength / 10f).roundToInt()).toString(),
                ) + "٪",
            )

            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = vm::reset) {
                Text("بازنشانی به حالت متعادل")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "افکت‌ها فقط روی خروجی SoundPilot اعمال می‌شوند و صدای سایر برنامه‌ها تغییر نمی‌کند. " +
                "برای جلوگیری از اعوجاج صدا، تقویت را متعادل نگه دارید.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BandsPanel(
    ui: EqualizerUiState,
    onBandChange: (Int, Float) -> Unit,
) {
    if (ui.gainsDb.isEmpty()) return
    val faderRange = EqualizerMath.uiRange(ui.minDb, ui.maxDb)

    // باندهای فرکانسی همیشه چپ‌به‌راست نمایش داده می‌شوند (استاندارد جهانی اکولایزر)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ui.gainsDb.forEachIndexed { index, gain ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = PersianText.formatSignedDb(gain),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    VerticalFader(
                        value = gain,
                        onValueChange = { onBandChange(index, it) },
                        valueRange = faderRange,
                        contentDescription = "باند فرکانسی ${PersianText.formatFrequency(ui.centersHz[index])}",
                        valueLabel = "${PersianText.formatSignedDb(gain)} دسی‌بل",
                    )
                    Text(
                        text = PersianText.formatFrequency(ui.centersHz[index]),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EffectSliderRow(
    title: String,
    supported: Boolean,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueText: String,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (supported) valueText else "پشتیبانی نمی‌شود",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = supported,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
