package com.soundpilot.app.ui.equalizer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soundpilot.app.audio.AudioDeviceCategory
import com.soundpilot.app.audio.EqualizerMath
import com.soundpilot.app.audio.EqualizerPresets
import com.soundpilot.app.audio.ReverbPreset
import com.soundpilot.app.audio.SoundPreset
import com.soundpilot.app.audio.SoundPresets
import com.soundpilot.app.audio.SoundTier
import com.soundpilot.app.ui.components.VerticalFader
import com.soundpilot.app.ui.theme.SpColors
import com.soundpilot.app.util.PersianText
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * استودیو صدا — ۲۰ پریست هماهنگ برای هر سیستم صوتی (ضعیف تا قوی) با:
 * اکولایزر گرافیکی، بیس، صدای فراگیر، تقویت بلندی، طنین سخت‌افزاری
 * و «حافظه خروجی»: هر خروجی (بلندگو/هدفون/بلوتوث) پریست مخصوص خودش را یاد می‌گیرد.
 */
@Composable
fun EffectStudioScreen() {
    val vm: EqualizerViewModel = viewModel()
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
    ) {
        // ───────────── هدر ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "استودیو صدا",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        ui.supported && ui.sessionActive && ui.enabled -> "فعال — روی نشست پخش زنده"
                        ui.supported && ui.sessionActive -> "متصل به پخش (خاموش)"
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
                colors = SwitchDefaults.colors(
                    checkedTrackColor = SpColors.Amber,
                    checkedThumbColor = Color(0xFF221600),
                ),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ───────────── کاروسل ۲۰ پریست ─────────────
        Text(
            text = "۲۰ پریست حرفه‌ای — برای سیستم ضعیف تا قوی",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(SoundPresets.all, key = { it.id }) { preset ->
                PresetCard(
                    preset = preset,
                    selected = ui.profileId == preset.id && ui.profileId != EqualizerPresets.CUSTOM_ID,
                    onClick = { vm.applyPreset(preset) },
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ───────────── توضیح پریست جاری ─────────────
        val current = ui.currentPreset
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SpColors.AmberGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        presetIcon(current.id),
                        contentDescription = null,
                        tint = Color(0xFF221600),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = current.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        TierBadge(current.tier)
                    }
                    Text(
                        text = current.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when {
            !ui.sessionActive -> {
                InfoCard(
                    title = "برای فعال‌سازی افکت‌ها، یک آهنگ پخش کنید",
                    body = "باندهای واقعی دستگاه پس از شروع پخش شناسایی می‌شوند و پریست انتخابی شما " +
                        "به‌طور خودکار اعمال و ذخیره می‌شود.",
                )
            }
            !ui.supported -> {
                InfoCard(
                    title = "موتور صوتی این دستگاه از افکت پشتیبانی نمی‌کند",
                    body = "پخش بدون تغییر و بدون خطا ادامه خواهد داشت؛ سایر امکانات برنامه فعال است.",
                )
            }
            else -> {
                // ───────────── نمودار زنده منحنی ─────────────
                CurveGraph(
                    curve = current.curve,
                    accent = SpColors.Amber,
                    enabled = ui.enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(14.dp))

                // ───────────── فیدرهای اکولایزر ─────────────
                Text(
                    text = "اکولایزر گرافیکی",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                BandsPanel(ui = ui, onBandChange = vm::setBandGain)
                Spacer(Modifier.height(16.dp))

                // ───────────── تقویت‌کننده‌ها ─────────────
                Column(Modifier.padding(horizontal = 16.dp)) {
                    EffectSliderRow(
                        title = "تقویت بیس",
                        supported = ui.bassSupported,
                        value = ui.bassStrength / 1000f,
                        onValueChange = { vm.setBassStrength((it * 1000).roundToInt()) },
                        valueText = PersianText.toPersianDigits(
                            ((ui.bassStrength / 10f).roundToInt()).toString(),
                        ) + "٪",
                    )
                    Spacer(Modifier.height(10.dp))
                    EffectSliderRow(
                        title = "صدای فراگیر (فضاسازی)",
                        supported = ui.virtualizerSupported,
                        value = ui.virtualizerStrength / 1000f,
                        onValueChange = { vm.setVirtualizerStrength((it * 1000).roundToInt()) },
                        valueText = PersianText.toPersianDigits(
                            ((ui.virtualizerStrength / 10f).roundToInt()).toString(),
                        ) + "٪",
                    )
                    Spacer(Modifier.height(10.dp))
                    EffectSliderRow(
                        title = "تقویت بلندی — مناسب ضبط ماشین",
                        supported = ui.loudnessSupported,
                        value = ui.loudnessGainMb / 1500f,
                        onValueChange = { vm.setLoudnessGainMb((it * 1500).roundToInt()) },
                        valueText = PersianText.formatSignedDb(ui.loudnessGainMb / 100f),
                    )
                }

                // ───────────── طنین (Reverb) ─────────────
                if (ui.reverbSupported) {
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "طنین سخت‌افزاری (Reverb)",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ReverbPreset.entries.forEach { rv ->
                                    FilterChip(
                                        selected = ui.reverbId == rv.id,
                                        onClick = { vm.setReverb(rv) },
                                        label = { Text(rv.label) },
                                    )
                                }
                            }
                        }
                        if (ui.reverbId != ReverbPreset.OFF.id) {
                            Spacer(Modifier.height(6.dp))
                            EffectSliderRow(
                                title = "میزان طنین",
                                supported = true,
                                value = ui.reverbAmount / 100f,
                                onValueChange = { vm.setReverbAmount((it * 100).roundToInt()) },
                                valueText = PersianText.toPersianDigits(ui.reverbAmount.toString()) + "٪",
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ───────────── حافظه خروجی ─────────────
        DeviceMemoryCard(
            ui = ui,
            onToggleAutoSwitch = vm::setAutoSwitch,
        )

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = vm::reset) {
                Text("بازنشانی به حالت خنثی")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "افکت‌ها فقط روی خروجی SoundPilot اعمال می‌شوند و صدای سایر برنامه‌ها تغییر نمی‌کند. " +
                "برای جلوگیری از اعوجاج صدا، تقویت را متعادل نگه دارید.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ───────────────────────── کارت پریست ─────────────────────────

@Composable
private fun PresetCard(
    preset: SoundPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = tween(200),
        label = "presetScale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) SpColors.Amber else Color.Transparent,
        animationSpec = tween(200),
        label = "presetBorder",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .scale(scale)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (selected) {
                        SpColors.AmberGradient
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        )
                    },
                )
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(22.dp),
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = presetIcon(preset.id),
                contentDescription = preset.label,
                tint = if (selected) Color(0xFF221600) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp),
            )
            // نقطه رنگی سطح سیستم در گوشه کارت
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(tierColor(preset.tier)),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = preset.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) SpColors.Amber else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(86.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TierBadge(tier: SoundTier) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tierColor(tier).copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = tier.label,
            style = MaterialTheme.typography.labelSmall,
            color = tierColor(tier),
        )
    }
}

private fun tierColor(tier: SoundTier): Color = when (tier) {
    SoundTier.ALL -> SpColors.Teal
    SoundTier.WEAK -> Color(0xFF9CD3A8)
    SoundTier.MEDIUM -> SpColors.Amber
    SoundTier.STRONG -> Color(0xFFFF8A65)
}

private fun presetIcon(presetId: String): ImageVector = when (presetId) {
    "flat" -> Icons.Rounded.Tune
    "car-economy", "car-standard", "car-pro" -> Icons.Rounded.DirectionsCar
    "bass-insane" -> Icons.Rounded.Waves
    "bass-clean" -> Icons.Rounded.WaterDrop
    "subwoofer" -> Icons.Rounded.Speaker
    "pop" -> Icons.Rounded.MusicNote
    "rock" -> Icons.Rounded.Bolt
    "metal" -> Icons.Rounded.Whatshot
    "jazz" -> Icons.Rounded.Coffee
    "classical" -> Icons.Rounded.AccountBalance
    "edm" -> Icons.Rounded.GraphicEq
    "hiphop" -> Icons.Rounded.Mic
    "acoustic" -> Icons.Rounded.Park
    "vocal" -> Icons.Rounded.RecordVoiceOver
    "late-night" -> Icons.Rounded.Nightlight
    "spatial3d" -> Icons.Rounded.SurroundSound
    "concert" -> Icons.Rounded.TheaterComedy
    "crystal" -> Icons.Rounded.AutoAwesome
    else -> Icons.Rounded.Tune
}

// ───────────────────────── نمودار منحنی ─────────────────────────

/** نمایش زنده منحنی پریست روی مقیاس لگاریتمی فرکانس */
@Composable
private fun CurveGraph(
    curve: List<Pair<Float, Float>>,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val lineColor = if (enabled) accent else MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)),
    ) {
        val minFreq = 20f
        val maxFreq = 20000f
        val minDb = -6f
        val maxDb = 6f
        val pad = 10.dp.toPx()

        fun xOf(freq: Float): Float {
            val t = (ln(freq.coerceIn(minFreq, maxFreq).toDouble()) - ln(minFreq.toDouble())) /
                (ln(maxFreq.toDouble()) - ln(minFreq.toDouble()))
            return pad + t.toFloat() * (size.width - 2 * pad)
        }

        fun yOf(db: Float): Float {
            val t = (db.coerceIn(minDb, maxDb) - minDb) / (maxDb - minDb)
            return size.height - pad - t.toFloat() * (size.height - 2 * pad)
        }

        // خط صفر
        drawLine(
            color = gridColor,
            start = Offset(pad, yOf(0f)),
            end = Offset(size.width - pad, yOf(0f)),
            strokeWidth = 1.dp.toPx(),
        )
        // خطوط راهنمای فرکانس
        listOf(100f, 1000f, 10000f).forEach { f ->
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(xOf(f), pad),
                end = Offset(xOf(f), size.height - pad),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // منحنی
        val path = Path()
        val samples = 64
        for (i in 0..samples) {
            val t = i.toFloat() / samples
            val logF = ln(minFreq.toDouble()) + t * (ln(maxFreq.toDouble()) - ln(minFreq.toDouble()))
            val freq = kotlin.math.exp(logF).toFloat()
            val y = yOf(EqualizerMath.sampleCurve(curve, freq))
            val x = xOf(freq)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

// ───────────────────────── فیدرهای باند ─────────────────────────

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
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ui.gainsDb.forEachIndexed { index, gain ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = PersianText.formatSignedDb(gain),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ui.enabled) SpColors.Amber else MaterialTheme.colorScheme.outline,
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
            colors = SliderDefaults.colors(
                activeTrackColor = SpColors.Amber,
                thumbColor = Color.White,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ───────────────────────── حافظه خروجی ─────────────────────────

@Composable
private fun DeviceMemoryCard(
    ui: EqualizerUiState,
    onToggleAutoSwitch: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = deviceIcon(ui.deviceCategory),
                    contentDescription = null,
                    tint = SpColors.Amber,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "حافظه خروجی — ${ui.deviceCategory.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "با هر بار وصل‌شدن این خروجی، پریست مخصوص همان دستگاه خودکار اعمال می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = ui.autoSwitch,
                    onCheckedChange = onToggleAutoSwitch,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = SpColors.Amber,
                        checkedThumbColor = Color(0xFF221600),
                    ),
                )
            }
            if (ui.autoSwitch && ui.savedForCurrentDevice) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SpColors.Teal.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "برای این خروجی ذخیره شد ✓",
                        style = MaterialTheme.typography.labelSmall,
                        color = SpColors.Teal,
                    )
                }
            }
        }
    }
}

private fun deviceIcon(category: AudioDeviceCategory): ImageVector = when (category) {
    AudioDeviceCategory.SPEAKER -> Icons.Rounded.Speaker
    AudioDeviceCategory.WIRED -> Icons.Rounded.Headphones
    AudioDeviceCategory.BLUETOOTH -> Icons.Rounded.Bluetooth
    AudioDeviceCategory.USB -> Icons.Rounded.Usb
    AudioDeviceCategory.HDMI -> Icons.Rounded.Tv
    AudioDeviceCategory.OTHER -> Icons.Rounded.QuestionMark
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
