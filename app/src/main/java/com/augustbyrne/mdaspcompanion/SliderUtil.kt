package com.augustbyrne.mdaspcompanion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow

@Composable
fun SliderWithLabel(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    logScale: Boolean = false,
    labelMinWidth: Dp = 24.dp
) {
    Column {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val offset = getSliderOffset(
                value = value,
                valueRange = valueRange,
                boxWidth = maxWidth,
                labelWidth = labelMinWidth + 8.dp
            )
/*
            val endValueText = if (!finiteEnd && value >= valueRange.endInclusive)
                "${value.toInt()} +" else value.toInt().toString()*/

            //SliderLabel(label = valueRange.start.toInt().toString(), minWidth = labelMinWidth)

            //if (value > valueRange.start) {
                SliderLabel(
                    label = if (logScale) 2f.pow(value).toInt().toString() else value.toInt().toString(),
                    minWidth = labelMinWidth,
                    modifier = Modifier
                        .padding(start = offset)
                )
            //}
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SliderLabel(
    label: String,
    minWidth: Dp,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,//Color.White,
        modifier = modifier
/*            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(4.dp)
            )*/
            .padding(4.dp)
            .defaultMinSize(minWidth = minWidth)
    )
}

private fun getSliderOffset(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    boxWidth: Dp,
    labelWidth: Dp
): Dp {
    val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val positionFraction = calcFraction(valueRange.start, valueRange.endInclusive, coerced)

    return (boxWidth - labelWidth) * positionFraction
}

private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f,1f)


@Composable
fun LabeledSlider(
    labels: List<String>,
    snapToLabels: Boolean = false,
    enabled: Boolean = true,
    invertDirection: Boolean = false,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0, value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val drawPadding = with(LocalDensity.current) { 16.dp.toPx() }
    val textSize = with(LocalDensity.current) { 12.dp.toPx() }
    val lineHeightDp = 14.dp
    val lineHeightPx = with(LocalDensity.current) { lineHeightDp.toPx() }
    val canvasHeight = 54.dp
    val textPaint = android.graphics.Paint().apply {
        color = if (isSystemInDarkTheme()) 0xffffffff.toInt() else 0xffff47586B.toInt()
        textAlign = android.graphics.Paint.Align.CENTER
        this.textSize = textSize
    }
    Box(contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .height(canvasHeight)
                .fillMaxWidth()
                .padding(
                    top = canvasHeight
                        .div(2)
                        .minus(lineHeightDp.div(2))
                )
        ) {
            val yStart = 0f
            val distance = (size.width.minus(2 * drawPadding)).div(labels.size.minus(1))
            labels.forEachIndexed { index, label ->
                drawLine(
                    color = Color.DarkGray,
                    start = Offset(x = drawPadding + index.times(distance), y = yStart),
                    end = Offset(x = drawPadding + index.times(distance), y = lineHeightPx)
                )
                if (index.rem(2) == 0) {
                    this.drawContext.canvas.nativeCanvas.drawText(
                        label,
                        drawPadding + index.times(distance),
                        size.height,
                        textPaint
                    )
                }
            }
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            valueRange = valueRange,
            enabled = enabled,
            steps = if (snapToLabels) labels.size.minus(2) else steps,
            colors = customSliderColors(invertDirection),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished
        )
    }
}

@Composable
private fun customSliderColors(invertDirection: Boolean): SliderColors {
    return if (invertDirection) {
        SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            inactiveTrackColor = MaterialTheme.colorScheme.primary,
            disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent)
    } else {
        SliderDefaults.colors(
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent)
    }
}
