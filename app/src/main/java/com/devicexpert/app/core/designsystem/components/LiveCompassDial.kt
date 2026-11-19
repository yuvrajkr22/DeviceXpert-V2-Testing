package com.devicexpert.app.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devicexpert.app.core.designsystem.CompassNorthRed
import com.devicexpert.app.core.designsystem.CompassSouthBlue
import com.devicexpert.app.core.designsystem.TextPrimary
import com.devicexpert.app.core.designsystem.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveCompassDial(
    azimuthDegrees: Float?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var cumulativeAngle by remember { mutableFloatStateOf(0f) }
    val targetRaw = azimuthDegrees ?: 0f

    LaunchedEffect(targetRaw) {
        val currentMod = ((cumulativeAngle % 360f) + 360f) % 360f
        var delta = targetRaw - currentMod
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        cumulativeAngle += delta
    }

    val animatedAngle by animateFloatAsState(
        targetValue = cumulativeAngle,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "CompassRotation"
    )

    val degreeLabelStyle = TextStyle(color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    val cardinalStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val radius = size.minDimension / 2f
        val cx = center.x
        val cy = center.y

        val crossArm = radius * 0.22f
        drawLine(TextPrimary.copy(alpha = 0.85f), Offset(cx - crossArm, cy), Offset(cx + crossArm, cy), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(TextPrimary.copy(alpha = 0.85f), Offset(cx, cy - crossArm), Offset(cx, cy + crossArm), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(TextPrimary, radius = 2.5.dp.toPx(), center = Offset(cx, cy - radius + 4.dp.toPx()))

        rotate(degrees = -animatedAngle, pivot = center) {
            val tickOuter = radius * 0.78f
            val majorTickInner = radius * 0.67f
            val minorTickInner = radius * 0.72f
            val degreeTextRadius = radius * 0.89f
            val cardinalRadius = radius * 0.54f

            for (deg in 0 until 360 step 3) {
                val rad = Math.toRadians((deg - 90).toDouble())
                val cosA = cos(rad).toFloat()
                val sinA = sin(rad).toFloat()
                val isMajor = deg % 30 == 0
                val isCardinal = deg % 90 == 0
                val innerR = if (isMajor) majorTickInner else minorTickInner
                val tickColor = when {
                    deg == 0 -> CompassNorthRed
                    deg == 180 -> CompassSouthBlue
                    isMajor -> TextPrimary
                    else -> TextSecondary.copy(alpha = 0.5f)
                }

                drawLine(
                    color = tickColor,
                    start = Offset(cx + innerR * cosA, cy + innerR * sinA),
                    end = Offset(cx + tickOuter * cosA, cy + tickOuter * sinA),
                    strokeWidth = if (isCardinal) 2.dp.toPx() else if (isMajor) 1.4.dp.toPx() else 0.8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                if (isMajor) {
                    val label = if (deg == 0) "360" else deg.toString()
                    val measured = textMeasurer.measure(label, degreeLabelStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(cx + degreeTextRadius * cosA - measured.size.width / 2f, cy + degreeTextRadius * sinA - measured.size.height / 2f)
                    )
                }
            }

            listOf(
                Triple("N", 0, CompassNorthRed),
                Triple("E", 90, TextPrimary),
                Triple("S", 180, CompassSouthBlue),
                Triple("W", 270, TextPrimary)
            ).forEach { (letter, deg, color) ->
                val rad = Math.toRadians((deg - 90).toDouble())
                val measured = textMeasurer.measure(letter, cardinalStyle.copy(color = color))
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(cx + cardinalRadius * cos(rad).toFloat() - measured.size.width / 2f, cy + cardinalRadius * sin(rad).toFloat() - measured.size.height / 2f)
                )
            }
        }
    }
}
