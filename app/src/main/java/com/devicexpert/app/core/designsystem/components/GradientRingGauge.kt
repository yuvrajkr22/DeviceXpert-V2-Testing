package com.devicexpert.app.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devicexpert.app.core.designsystem.CpuBlue
import com.devicexpert.app.core.designsystem.CpuMagenta
import com.devicexpert.app.core.designsystem.InactiveTrack

@Composable
fun GradientRingGauge(
    progress: Float?,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(CpuBlue, CpuMagenta, CpuBlue),
    trackColor: Color = InactiveTrack.copy(alpha = 0.45f),
    strokeWidth: Dp = 5.dp,
    labelText: String? = null,
    fontSize: TextUnit = 13.sp
) {
    val safeTarget = (progress ?: 0f).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = safeTarget * 360f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "GradientRingSweep"
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(
                width = (size.width - strokePx).coerceAtLeast(1f),
                height = (size.height - strokePx).coerceAtLeast(1f)
            )
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            if (animatedSweep > 0f) {
                rotate(degrees = -90f) {
                    drawArc(
                        brush = Brush.sweepGradient(colors = gradientColors),
                        startAngle = 0f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
        }

        val displayText = labelText ?: if (progress != null) "${(safeTarget * 100).toInt()}%" else "N/A"
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
