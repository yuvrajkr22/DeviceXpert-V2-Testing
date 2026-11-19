package com.devicexpert.app.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devicexpert.app.core.designsystem.CharcoalElevated
import com.devicexpert.app.core.designsystem.OledBlack
import com.devicexpert.app.core.designsystem.TextSecondary
import com.devicexpert.app.domain.model.device.DeviceCategory

@Composable
fun DeviceWireframeVisual(
    category: DeviceCategory,
    primaryLabel: String,
    secondaryLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bezelColor = OledBlack.copy(alpha = 0.85f)
            val glassFill = CharcoalElevated.copy(alpha = 0.72f)
            val accentStroke = Color(0xFF3E424D)

            when (category) {
                DeviceCategory.ANDROID -> {
                    val outerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                    val innerInset = 5.dp.toPx()
                    val innerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())

                    drawRoundRect(color = glassFill, topLeft = Offset.Zero, size = size, cornerRadius = outerRadius)
                    drawRoundRect(
                        color = bezelColor,
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                        cornerRadius = outerRadius,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawRoundRect(
                        color = accentStroke,
                        topLeft = Offset(innerInset, innerInset),
                        size = Size((w - innerInset * 2).coerceAtLeast(1f), (h - innerInset * 2).coerceAtLeast(1f)),
                        cornerRadius = innerRadius,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawLine(
                        color = bezelColor.copy(alpha = 0.65f),
                        start = Offset(w * 0.72f, h * 0.30f),
                        end = Offset(w * 0.89f, h * 0.14f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bezelColor.copy(alpha = 0.65f),
                        start = Offset(w * 0.11f, h * 0.84f),
                        end = Offset(w * 0.32f, h * 0.64f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                DeviceCategory.COMPUTER -> {
                    val screenH = h * 0.76f
                    val radius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    drawRoundRect(color = glassFill, topLeft = Offset.Zero, size = Size(w, screenH), cornerRadius = radius)
                    drawRoundRect(
                        color = bezelColor,
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(w - 2.dp.toPx(), screenH - 2.dp.toPx()),
                        cornerRadius = radius,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawLine(accentStroke, Offset(w * 0.5f, screenH), Offset(w * 0.5f, h * 0.93f), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(accentStroke, Offset(w * 0.28f, h * 0.93f), Offset(w * 0.72f, h * 0.93f), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                }

                DeviceCategory.IOT -> {
                    val pad = 10.dp.toPx()
                    val radius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    val pinCount = 6
                    val pinStep = (h - pad * 4) / (pinCount - 1).coerceAtLeast(1)
                    for (i in 0 until pinCount) {
                        val y = pad * 2 + i * pinStep
                        drawLine(accentStroke, Offset(0f, y), Offset(pad, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(accentStroke, Offset(w - pad, y), Offset(w, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                    }
                    drawRoundRect(
                        color = glassFill,
                        topLeft = Offset(pad, pad),
                        size = Size((w - pad * 2).coerceAtLeast(1f), (h - pad * 2).coerceAtLeast(1f)),
                        cornerRadius = radius
                    )
                    drawRoundRect(
                        color = bezelColor,
                        topLeft = Offset(pad, pad),
                        size = Size((w - pad * 2).coerceAtLeast(1f), (h - pad * 2).coerceAtLeast(1f)),
                        cornerRadius = radius,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Text(
                text = secondaryLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
