package com.example.myvolumizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.getValue
@Composable
fun VolumeKnob(
    volume: Int,
    onVolumeChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    val animatedSweep by animateFloatAsState(
        targetValue = volume / 100f * 360f,
        animationSpec = tween(durationMillis = 300)
    )

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val touchX = change.position.x - size.width / 2
                val touchY = change.position.y - size.height / 2

                var angle = Math.toDegrees(atan2(touchY.toDouble(), touchX.toDouble()))
                angle += 90
                if (angle < 0) angle += 360.0

                val newVolume = angle.coerceIn(0.0, 360.0).let { ((it / 360.0) * 100).toInt() }
                onVolumeChange(newVolume)
            }
        }
    ) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Outer metallic ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.LightGray, Color.DarkGray),
                center = center,
                radius = radius
            ),
            radius = radius,
            style = Stroke(width = 20f)
        )

        // Inner ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFCFD8DC), Color(0xFF78909C)),
                center = center,
                radius = radius - 10f
            ),
            radius = radius - 10f,
            style = Stroke(width = 4f)
        )

        // Knob center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.DarkGray, Color.Black),
                center = center,
                radius = 16f
            ),
            radius = 16f
        )

        // Ticks with major/minor distinction
        val tickCount = 20
        val tickLengthMajor = 16f
        val tickLengthMinor = 8f
        val tickWidth = 2f
        val filledTicks = (tickCount * volume / 100f).toInt()

        for (i in 0..tickCount) {
            val tickAngle = PI * 2 * (i / tickCount.toDouble()) - PI / 2
            val tickLength = if (i % 5 == 0) tickLengthMajor else tickLengthMinor
            val startX = center.x + (radius - tickLength - 10f) * cos(tickAngle).toFloat()
            val startY = center.y + (radius - tickLength - 10f) * sin(tickAngle).toFloat()
            val endX = center.x + (radius - 10f) * cos(tickAngle).toFloat()
            val endY = center.y + (radius - 10f) * sin(tickAngle).toFloat()
            drawLine(
                color = if (i <= filledTicks) primaryColor else Color.LightGray,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = tickWidth
            )
        }

        // Needle with glow
        val startAngle = -90f
        val angleRad = Math.toRadians((startAngle + animatedSweep).toDouble())
        val indicatorLength = radius * 0.8f
        val x = center.x + indicatorLength * cos(angleRad).toFloat()
        val y = center.y + indicatorLength * sin(angleRad).toFloat()

        drawLine(
            brush = Brush.radialGradient(
                colors = listOf(Color.Red, Color(0xFFFF5252)),
                center = center,
                radius = indicatorLength
            ),
            start = center,
            end = Offset(x, y),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
    }
}
