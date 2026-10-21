package com.soundpilot.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soundpilot.app.data.AlbumArtRepository
import com.soundpilot.app.ui.theme.SpColors
import kotlin.math.min
import kotlin.math.sin

/**
 * پس‌زمینه «شفق» — دو هاله رنگی که آرام روی صفحه حرکت می‌کنند.
 * سبک است (چند گرادیان شعاعی) و به صفحه پخش و استودیو عمق مدرن می‌دهد.
 */
@Composable
fun AuroraBackdrop(
    modifier: Modifier = Modifier,
    tintA: Color = SpColors.Amber,
    tintB: Color = SpColors.Teal,
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
        label = "auroraA",
    )
    val phase2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(19_000, easing = LinearEasing), RepeatMode.Restart),
        label = "auroraB",
    )

    val baseColor = MaterialTheme.colorScheme.background
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = baseColor)
        drawAuroraBlob(
            center = Offset(
                x = size.width * (0.5f + 0.42f * sin(phase1)),
                y = size.height * (0.18f + 0.10f * sin(phase1 * 0.7f)),
            ),
            radius = size.minDimension * 0.9f,
            color = tintA,
            alpha = 0.16f,
        )
        drawAuroraBlob(
            center = Offset(
                x = size.width * (0.5f + 0.38f * sin(phase2 + 2.1f)),
                y = size.height * (0.82f + 0.10f * sin(phase2 * 0.9f + 1f)),
            ),
            radius = size.minDimension * 0.8f,
            color = tintB,
            alpha = 0.12f,
        )
    }
}

private fun DrawScope.drawAuroraBlob(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * لایه شیشه‌ای — سطح نیمه‌شفاف با لبه ظریف برای چیدمان مدرن روی پس‌زمینه‌های رنگی.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    fillAlpha: Float = 0.06f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = fillAlpha))
            .background(SpColors.GlassFill)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.04f), Color.Transparent),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            SpColors.GlassBorder,
                            SpColors.GlassBorder,
                        ),
                    ),
                ),
        )
        Box(Modifier.fillMaxSize()) { content() }
    }
}

/** نمایشگر میله‌های پخش زنده — نشانگر بصری «این آهنگ در حال پخش است» */
@Composable
fun PlayingBarsIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 4.dp,
    barMaxHeight: Dp = 18.dp,
) {
    val transition = rememberInfiniteTransition(label = "bars")
    val spec = infiniteRepeatable<Float>(
        animation = tween(420, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
    )
    val b1 by transition.animateFloat(0.35f, 1f, spec, label = "b1")
    val b2 by transition.animateFloat(0.9f, 0.3f, spec, label = "b2")
    val b3 by transition.animateFloat(0.5f, 1f, spec, label = "b3")
    val b4 by transition.animateFloat(1f, 0.45f, spec, label = "b4")

    Canvas(modifier = modifier.width(barWidth * 7).height(barMaxHeight)) {
        val widths = barWidth.toPx()
        val gap = widths * 1f
        val maxH = size.height
        val animatedLevels = listOf(b1, b2, b3, b4)
        val staticLevels = listOf(0.45f, 0.8f, 0.55f, 0.7f)
        animatedLevels.forEachIndexed { i, _ ->
            val level = if (isPlaying) animatedLevels[i] else staticLevels[i] * 0.35f
            val h = maxH * level.coerceIn(0.15f, 1f)
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (widths + gap), maxH - h),
                size = androidx.compose.ui.geometry.Size(widths, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(widths / 2f, widths / 2f),
            )
        }
    }
}

/**
 * پس‌زمینه تمام‌صفحه آرت آلبوم با بلور — امضای پلیرهای مدرن.
 * بیت‌مپ به اندازه خیلی کوچک پایین‌نمونه‌گیری می‌شود و با کشیدگی نمایش داده می‌شود؛
 * این خودش یک بلور ارزان و همه‌جاکار است (بدون RenderEffect و سازگار با API ۲۶).
 */
@Composable
fun BlurredArtBackdrop(
    albumId: Long,
    songId: Long,
    modifier: Modifier = Modifier,
    scrimColor: Color = SpColors.NightBase,
) {
    var small by remember(albumId) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(albumId, songId) {
        small = null
        if (albumId > 0) {
            val full = AlbumArtRepository.bitmapFor(albumId, songId)
            small = full?.let { src ->
                val target = 42
                val w = src.width.coerceAtLeast(1)
                val h = src.height.coerceAtLeast(1)
                val scale = 1f / (min(w, h).toFloat() / target).coerceAtLeast(1f)
                android.graphics.Bitmap.createScaledBitmap(
                    src,
                    (w * scale).toInt().coerceAtLeast(2),
                    (h * scale).toInt().coerceAtLeast(2),
                    true,
                )
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        val bmp = small
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // اسکریم برای خوانایی متن و یکدست‌کردن رنگ با تم
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scrimColor.copy(alpha = 0.62f),
                            scrimColor.copy(alpha = 0.86f),
                            scrimColor,
                        ),
                    ),
                ),
        )
    }
}

/** آواتار دایره‌ای خواننده با حرف اول */
@Composable
fun ArtistAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = name.trim().take(1).ifEmpty { "؟" }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(listOf(SpColors.Amber.copy(alpha = 0.35f), SpColors.Teal.copy(alpha = 0.35f))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** فاصله استاندارد افقی ردیف‌ها */
@Composable
fun RowSpacer(width: Dp = 12.dp) = Spacer(Modifier.width(width))
