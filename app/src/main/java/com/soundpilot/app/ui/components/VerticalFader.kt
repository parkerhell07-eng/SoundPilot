package com.soundpilot.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * اسلایدر عمودی (فیدر) سفارشی برای باندهای اکولایزر.
 * با draw مستقیم و ژست کشیدن عمودی — رفتار قطعی و راست‌چین‌دوست.
 * راست‌به‌چپ: مقدار صفر پایین، تقویت بالا (استاندارد همه اکولایزرها).
 */
@Composable
fun VerticalFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "",
    valueLabel: String = "",
) {
    var heightPx by remember { mutableIntStateOf(1) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val thumbColor = MaterialTheme.colorScheme.onPrimary

    fun updateFromY(y: Float) {
        if (heightPx <= 1) return
        val fraction = (1f - (y / heightPx)).coerceIn(0f, 1f)
        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
        onValueChange(newValue)
    }

    val semanticsModifier = if (contentDescription.isEmpty()) {
        Modifier
    } else {
        Modifier.semantics {
            this.contentDescription = contentDescription
            stateDescription = valueLabel
        }
    }

    Canvas(
        modifier = modifier
            .height(170.dp)
            .width(52.dp)
            .then(semanticsModifier)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { offset -> updateFromY(offset.y) },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        updateFromY(change.position.y)
                    },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset -> updateFromY(offset.y) }
            },
    ) {
        heightPx = size.height.toInt()
        val cx = size.width / 2f
        val trackWidth = 8.dp.toPx()
        val corner = CornerRadius(trackWidth / 2f, trackWidth / 2f)

        // شیار
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(cx - trackWidth / 2f, 0f),
            size = Size(trackWidth, size.height),
            cornerRadius = corner,
        )

        val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
        val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)
        val thumbY = size.height * (1f - fraction)

        // بخش فعال
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(cx - trackWidth / 2f, thumbY),
            size = Size(trackWidth, size.height - thumbY),
            cornerRadius = corner,
        )

        // دستگیره
        drawCircle(
            color = activeColor,
            radius = 12.dp.toPx(),
            center = Offset(cx, thumbY),
        )
        drawCircle(
            color = thumbColor,
            radius = 5.dp.toPx(),
            center = Offset(cx, thumbY),
        )
    }
}

