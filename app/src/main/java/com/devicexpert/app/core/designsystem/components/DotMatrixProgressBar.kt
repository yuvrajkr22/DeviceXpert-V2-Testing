package com.devicexpert.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devicexpert.app.core.designsystem.CyanPulse
import com.devicexpert.app.core.designsystem.InactiveTrack
import kotlin.math.roundToInt

@Composable
fun DotMatrixProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    dotCount: Int = 14,
    activeColor: Color = CyanPulse,
    inactiveColor: Color = InactiveTrack,
    dotSize: Dp = 6.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "DotMatrixProgress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(dotSize + 4.dp)
    ) {
        if (dotCount <= 0) return@Canvas
        val radius = dotSize.toPx() / 2f
        val activeDots = (animatedProgress * dotCount).roundToInt().coerceIn(0, dotCount)
        val usableWidth = (size.width - radius * 2f).coerceAtLeast(1f)
        val step = if (dotCount > 1) usableWidth / (dotCount - 1) else 0f
        val centerY = size.height / 2f

        for (i in 0 until dotCount) {
            val cx = radius + (i * step)
            drawCircle(
                color = if (i < activeDots) activeColor else inactiveColor,
                radius = radius,
                center = Offset(cx, centerY)
            )
        }
    }
}
