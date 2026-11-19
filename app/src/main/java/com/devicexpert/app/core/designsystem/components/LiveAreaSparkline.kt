package com.devicexpert.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devicexpert.app.core.designsystem.CpuBlue
import com.devicexpert.app.core.designsystem.CpuMagenta
import kotlin.math.max

data class SparklineSeries(
    val values: List<Float>,
    val lineColors: List<Color>,
    val fillStartColor: Color? = null
)

@Composable
fun LiveAreaSparkline(
    series: List<SparklineSeries>,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    strokeWidth: Dp = 2.dp,
    fixedMin: Float? = 0f,
    fixedMax: Float? = null
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas

        val allValues = series.flatMap { it.values }
        val minVal = fixedMin ?: (allValues.minOrNull() ?: 0f)
        val rawMax = fixedMax ?: (allValues.maxOrNull() ?: 1f)
        val maxVal = max(rawMax, minVal + 1f)
        val range = (maxVal - minVal).coerceAtLeast(0.001f)
        val topPad = strokeWidth.toPx() * 2f
        val bottomPad = strokeWidth.toPx()
        val usableHeight = (h - topPad - bottomPad).coerceAtLeast(1f)

        series.forEach { s ->
            val pts = if (s.values.size >= 2) s.values else listOf(minVal, minVal)
            val stepX = w / (pts.size - 1).coerceAtLeast(1)

            val linePath = Path()
            val fillPath = Path()

            pts.forEachIndexed { index, value ->
                val x = index * stepX
                val normalized = ((value - minVal) / range).coerceIn(0f, 1f)
                val y = topPad + usableHeight * (1f - normalized)

                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (index - 1) * stepX
                    val prevNorm = ((pts[index - 1] - minVal) / range).coerceIn(0f, 1f)
                    val prevY = topPad + usableHeight * (1f - prevNorm)
                    val controlX = (prevX + x) / 2f
                    linePath.cubicTo(controlX, prevY, controlX, y, x, y)
                    fillPath.cubicTo(controlX, prevY, controlX, y, x, y)
                }
            }

            if (s.fillStartColor != null) {
                fillPath.lineTo(w, h)
                fillPath.close()
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(s.fillStartColor, Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )
            }

            val lineBrush = if (s.lineColors.size >= 2) {
                Brush.horizontalGradient(colors = s.lineColors)
            } else {
                val c = s.lineColors.firstOrNull() ?: CpuBlue
                Brush.horizontalGradient(colors = listOf(c, c))
            }

            drawPath(
                path = linePath,
                brush = lineBrush,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun CpuAreaSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    LiveAreaSparkline(
        series = listOf(
            SparklineSeries(
                values = values,
                lineColors = listOf(CpuMagenta, CpuBlue),
                fillStartColor = CpuBlue.copy(alpha = 0.30f)
            )
        ),
        modifier = modifier,
        height = height,
        fixedMin = 0f,
        fixedMax = 100f
    )
}
