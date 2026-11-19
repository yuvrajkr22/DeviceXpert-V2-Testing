package com.devicexpert.app.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.devicexpert.app.core.designsystem.InactiveTrack
import com.devicexpert.app.core.designsystem.StorageAmber
import com.devicexpert.app.core.designsystem.StorageGreen
import com.devicexpert.app.core.designsystem.StorageOrange

@Composable
fun ConcentricStorageRings(
    outerProgress: Float,
    middleProgress: Float,
    innerProgress: Float,
    modifier: Modifier = Modifier,
    outerColor: Color = StorageAmber,
    middleColor: Color = StorageOrange,
    innerColor: Color = StorageGreen
) {
    val animOuter by animateFloatAsState(
        targetValue = outerProgress.coerceIn(0f, 1f),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "OuterStorageRing"
    )
    val animMiddle by animateFloatAsState(
        targetValue = middleProgress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "MiddleStorageRing"
    )
    val animInner by animateFloatAsState(
        targetValue = innerProgress.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "InnerStorageRing"
    )

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val minDim = size.minDimension
        val strokePx = (minDim * 0.095f).coerceAtLeast(4.dp.toPx())
        val gapPx = strokePx * 0.42f

        val rings = listOf(
            Triple(animOuter, outerColor, 0),
            Triple(animMiddle, middleColor, 1),
            Triple(animInner, innerColor, 2)
        )

        rings.forEach { (progress, color, index) ->
            val inset = (strokePx / 2f) + index * (strokePx + gapPx)
            val diameter = (minDim - inset * 2f).coerceAtLeast(2f)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = InactiveTrack.copy(alpha = 0.35f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        drawCircle(
            color = innerColor,
            radius = (minDim * 0.055f).coerceAtLeast(3.dp.toPx()),
            center = center
        )
    }
}
